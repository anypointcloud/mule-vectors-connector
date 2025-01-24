package org.mule.extension.vectors.internal.model.text.vertexai;

import com.google.auth.Credentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.aiplatform.v1beta1.*;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static dev.langchain4j.internal.Json.toJson;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;
import static java.util.stream.Collectors.toList;

/**
 * A model class for interacting with the Vertex AI Embedding service.
 * This model generates embeddings for text segments using Google's Vertex AI platform.
 */
public class VertexAiEmbeddingModel extends DimensionAwareEmbeddingModel {

  private static final String DEFAULT_GOOGLEAPIS_ENDPOINT_SUFFIX = "-aiplatform.googleapis.com:443";
  private static final int COMPUTE_TOKENS_MAX_INPUTS_PER_REQUEST = 2_048;
  private static final int DEFAULT_MAX_SEGMENTS_PER_BATCH = 250;
  private static final int DEFAULT_MAX_TOKENS_PER_BATCH = 20_000;

  private final PredictionServiceSettings settings;
  private final LlmUtilityServiceSettings llmUtilitySettings;
  private final EndpointName endpointName;
  private final Integer maxRetries;
  private final Integer maxSegmentsPerBatch;
  private final Integer maxTokensPerBatch;
  private final TaskType taskType;
  private final String titleMetadataKey;
  private final Integer outputDimensionality;
  private final Boolean autoTruncate;

  /**
   * Constructs a VertexAiEmbeddingModel instance.
   *
   * @param endpoint                The API endpoint (optional).
   * @param project                 The GCP project ID.
   * @param credentials             The credentials to authenticate the service account.
   * @param location                The location of the Vertex AI service.
   * @param publisher               The publisher of the model.
   * @param modelName               The name of the model.
   * @param maxRetries              The maximum number of retries for requests.
   * @param maxSegmentsPerBatch     The maximum number of segments to process in a batch.
   * @param maxTokensPerBatch       The maximum number of tokens to process in a batch.
   * @param taskType                The task type for embedding (e.g., classification, semantic similarity).
   * @param titleMetadataKey        The key used to extract metadata for titles (optional).
   * @param outputDimensionality    The dimensionality of the output embeddings.
   * @param autoTruncate            Whether to automatically truncate the embeddings if they exceed a length limit.
   */
  public VertexAiEmbeddingModel(String endpoint,
                                String project,
                                Credentials credentials,
                                String location,
                                String publisher,
                                String modelName,
                                Integer maxRetries,
                                Integer maxSegmentsPerBatch,
                                Integer maxTokensPerBatch,
                                TaskType taskType,
                                String titleMetadataKey,
                                Integer outputDimensionality,
                                Boolean autoTruncate) {

    String regionWithBaseAPI = endpoint != null ? endpoint :
        ensureNotBlank(location, "location") + DEFAULT_GOOGLEAPIS_ENDPOINT_SUFFIX;

    this.endpointName = EndpointName.ofProjectLocationPublisherModelName(
        ensureNotBlank(project, "project"),
        location,
        ensureNotBlank(publisher, "publisher"),
        ensureNotBlank(modelName, "modelName")
    );

    try {

      if(credentials != null) {

        this.settings = PredictionServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .setEndpoint(regionWithBaseAPI)
            .build();
        this.llmUtilitySettings = LlmUtilityServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .setEndpoint(regionWithBaseAPI)
            .build();

      } else {

        this.settings = ((PredictionServiceSettings.Builder) PredictionServiceSettings.newBuilder()
            .setEndpoint(regionWithBaseAPI)).build();
        this.llmUtilitySettings = ((LlmUtilityServiceSettings.Builder)LlmUtilityServiceSettings.newBuilder()
            .setEndpoint(this.settings.getEndpoint())).build();
      }

    } catch (IOException ioException) {

      throw new RuntimeException(ioException);
    }

    this.maxRetries = getOrDefault(maxRetries, 3);

    this.maxSegmentsPerBatch = ensureGreaterThanZero(
        getOrDefault(maxSegmentsPerBatch, DEFAULT_MAX_SEGMENTS_PER_BATCH), "maxSegmentsPerBatch");
    this.maxTokensPerBatch = ensureGreaterThanZero(
        getOrDefault(maxTokensPerBatch, DEFAULT_MAX_TOKENS_PER_BATCH), "maxTokensPerBatch");

    this.taskType = taskType;
    this.titleMetadataKey = getOrDefault(titleMetadataKey, "title");

    this.outputDimensionality = outputDimensionality;
    this.autoTruncate = getOrDefault(autoTruncate, false);
  }

  /**
   * Embeds all the provided text segments.
   *
   * @param segments The list of text segments to embed.
   * @return A response containing the embeddings and token usage statistics.
   */
  public Response<List<Embedding>> embedAll(List<TextSegment> segments) {

    try (PredictionServiceClient client = PredictionServiceClient.create(settings)) {

      List<Embedding> embeddings = new ArrayList<>();
      int inputTokenCount = 0;

      List<Integer> tokensCounts = this.calculateTokensCounts(segments);
      List<Integer> batchSizes = groupByBatches(tokensCounts);

      for (int i = 0, j = 0; i < segments.size() && j < batchSizes.size(); i += batchSizes.get(j), j++) {

        List<TextSegment> batch = segments.subList(i, i + batchSizes.get(j));

        List<Value> instances = new ArrayList<>();
        for (TextSegment segment : batch) {
          VertexAiEmbeddingInstance embeddingInstance = new VertexAiEmbeddingInstance(segment.text());
          // Specify the type of embedding task when specified
          if (this.taskType != null) {
            embeddingInstance.setTaskType(taskType);
            if (this.taskType.equals(TaskType.RETRIEVAL_DOCUMENT)) {
              // Title metadata is used for calculating embeddings for document retrieval
              embeddingInstance.setTitle(segment.metadata().getString(titleMetadataKey));
            }
          }

          Value.Builder instanceBuilder = Value.newBuilder();
          JsonFormat.parser().merge(toJson(embeddingInstance), instanceBuilder);
          instances.add(instanceBuilder.build());
        }

        VertexAiEmbeddingParameters parameters = new VertexAiEmbeddingParameters(
            outputDimensionality, getOrDefault(autoTruncate, false));
        Value.Builder parameterBuilder = Value.newBuilder();
        JsonFormat.parser().merge(toJson(parameters), parameterBuilder);

        PredictResponse response = withRetry(() -> client.predict(endpointName, instances, parameterBuilder.build()), maxRetries);

        embeddings.addAll(response.getPredictionsList().stream()
                              .map(VertexAiEmbeddingModel::toEmbedding)
                              .collect(toList()));

        for (Value prediction : response.getPredictionsList()) {
          inputTokenCount += extractTokenCount(prediction);
        }
      }

      return Response.from(
          embeddings,
          new TokenUsage(inputTokenCount)
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates the token counts for each text segment.
   *
   * @param segments The list of text segments.
   * @return A list of token counts for each segment.
   */
  public List<Integer> calculateTokensCounts(List<TextSegment> segments) {

    try (LlmUtilityServiceClient utilClient = LlmUtilityServiceClient.create(this.llmUtilitySettings)) {
      List<Integer> tokensCounts = new ArrayList<>();

      // The computeTokens endpoint has a limit of up to 2048 input texts per request
      for (int i = 0; i < segments.size(); i += COMPUTE_TOKENS_MAX_INPUTS_PER_REQUEST) {
        List<TextSegment> batch = segments.subList(i,
                                                   Math.min(i + COMPUTE_TOKENS_MAX_INPUTS_PER_REQUEST, segments.size()));

        List<Value> instances = new ArrayList<>();
        for (TextSegment segment : batch) {
          Value.Builder instanceBuilder = Value.newBuilder();
          JsonFormat.parser().merge(toJson(new VertexAiEmbeddingInstance(segment.text())), instanceBuilder);
          instances.add(instanceBuilder.build());
        }

        //computeTokens it's a utility endpoint that's free to use. It's primarily used for planning and optimization purposes,
        //like determining batch sizes and estimating token usage.

        ComputeTokensRequest computeTokensRequest = ComputeTokensRequest.newBuilder()
            .setEndpoint(endpointName.toString())
            .addAllInstances(instances)
            .build();

        ComputeTokensResponse computeTokensResponse = utilClient.computeTokens(computeTokensRequest);

        tokensCounts.addAll(computeTokensResponse
                                .getTokensInfoList()
                                .stream()
                                .map(TokensInfo::getTokensCount)
                                .collect(toList()));
      }

      return tokensCounts;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Integer knownDimension() {
    return VertexAiEmbeddingModelName.knownDimension(this.endpointName.getModel());
  }

  private List<Integer> groupByBatches(List<Integer> tokensCounts) {

    // create a list of sublists of tokens counts
    // where the maximum number of text segments per sublist is 250
    // and the sum of the tokens counts in each sublist is less than 20_000

    List<List<Integer>> batches = new ArrayList<>();

    List<Integer> currentBatch = new ArrayList<>();
    int currentBatchSum = 0;

    for (Integer tokensCount : tokensCounts) {
      if (currentBatchSum + tokensCount <= maxTokensPerBatch &&
          currentBatch.size() < maxSegmentsPerBatch) {
        currentBatch.add(tokensCount);
        currentBatchSum += tokensCount;
      } else {
        batches.add(currentBatch);
        currentBatch = new ArrayList<>();
        currentBatch.add(tokensCount);
        currentBatchSum = tokensCount;
      }
    }

    if (!currentBatch.isEmpty()) {
      batches.add(currentBatch);
    }

    // returns the list of number of text segments for each batch of embedding calculations

    return batches.stream()
        .mapToInt(List::size)
        .boxed()
        .collect(toList());
  }

  private static Embedding toEmbedding(Value prediction) {

    List<Float> vector = prediction.getStructValue()
        .getFieldsMap()
        .get("embeddings")
        .getStructValue()
        .getFieldsOrThrow("values")
        .getListValue()
        .getValuesList()
        .stream()
        .map(v -> (float) v.getNumberValue())
        .collect(toList());

    return Embedding.from(vector);
  }

  private static int extractTokenCount(Value prediction) {

    return (int) prediction.getStructValue()
        .getFieldsMap()
        .get("embeddings")
        .getStructValue()
        .getFieldsMap()
        .get("statistics")
        .getStructValue()
        .getFieldsMap()
        .get("token_count")
        .getNumberValue();
  }

  /**
   * Returns a builder for the VertexAiEmbeddingModel.
   *
   * @return A VertexAiEmbeddingModel builder.
   */
  public static VertexAiEmbeddingModel.Builder builder() {
    for (VertexAiEmbeddingModelBuilderFactory factory : loadFactories(VertexAiEmbeddingModelBuilderFactory.class)) {
      return factory.get();
    }
    return new Builder();
  }

  /**
   * Enum representing different task types supported by Vertex AI embeddings.
   */
  public static enum TaskType {
    RETRIEVAL_QUERY,
    RETRIEVAL_DOCUMENT,
    SEMANTIC_SIMILARITY,
    CLASSIFICATION,
    CLUSTERING,
    QUESTION_ANSWERING,
    FACT_VERIFICATION,
    CODE_RETRIEVAL_QUERY;
  }

  /**
   * Builder class for constructing a VertexAiEmbeddingModel.
   */
  public static class Builder {
    private String endpoint;
    private String project;
    private Credentials credentials;
    private String location;
    private String publisher;
    private String modelName;
    private Integer maxRetries;
    private Integer maxSegmentsPerBatch;
    private Integer maxTokensPerBatch;
    private TaskType taskType;
    private String titleMetadataKey;
    private Integer outputDimensionality;
    private Boolean autoTruncate;

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder project(String project) {
      this.project = project;
      return this;
    }

    public Builder credentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder publisher(String publisher) {
      this.publisher = publisher;
      return this;
    }

    public Builder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public Builder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder maxSegmentsPerBatch(Integer maxSegmentsPerBatch) {
      this.maxSegmentsPerBatch = maxSegmentsPerBatch;
      return this;
    }

    public Builder maxTokensPerBatch(Integer maxTokensPerBatch) {
      this.maxTokensPerBatch = maxTokensPerBatch;
      return this;
    }

    public Builder taskType(TaskType taskType) {
      this.taskType = taskType;
      return this;
    }

    public Builder titleMetadataKey(String titleMetadataKey) {
      this.titleMetadataKey = titleMetadataKey;
      return this;
    }

    public Builder autoTruncate(Boolean autoTruncate) {
      this.autoTruncate = autoTruncate;
      return this;
    }

    public Builder outputDimensionality(Integer outputDimensionality) {
      this.outputDimensionality = outputDimensionality;
      return this;
    }

    public VertexAiEmbeddingModel build() {
      return new VertexAiEmbeddingModel(
          endpoint,
          project,
          credentials,
          location,
          publisher,
          modelName,
          maxRetries,
          maxSegmentsPerBatch,
          maxTokensPerBatch,
          taskType,
          titleMetadataKey,
          outputDimensionality,
          autoTruncate
      );
    }
  }

  /**
   * Helper class for representing embedding instances.
   */
  class VertexAiEmbeddingInstance {
    private String content;
    private String title;
    private TaskType task_type;

    VertexAiEmbeddingInstance(String content) {
      this.content = content;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public void setTaskType(TaskType taskType) {
      this.task_type = taskType;
    }
  }

  /**
   * Helper class for representing embedding parameters.
   */
  class VertexAiEmbeddingParameters {
    private Integer outputDimensionality;
    private Boolean autoTruncate;

    VertexAiEmbeddingParameters(Integer outputDimensionality, Boolean autoTruncate) {
      this.outputDimensionality = outputDimensionality;
      this.autoTruncate = autoTruncate;
    }

    public Integer getOutputDimensionality() {
      return this.outputDimensionality;
    }

    public Boolean isAutoTruncate() {
      return this.autoTruncate;
    }
  }

  /**
   * Interface for factories that produce VertexAiEmbeddingModel builders.
   */
  interface VertexAiEmbeddingModelBuilderFactory extends Supplier<Builder> {
  }
}
