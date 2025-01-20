package org.mule.extension.vectors.internal.model.vertexai;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.aiplatform.v1beta1.ComputeTokensRequest;
import com.google.cloud.aiplatform.v1beta1.ComputeTokensResponse;
import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.LlmUtilityServiceClient;
import com.google.cloud.aiplatform.v1beta1.LlmUtilityServiceSettings;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.cloud.aiplatform.v1beta1.TokensInfo;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.RetryUtils;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.spi.ServiceHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VertexAiEmbeddingModel extends DimensionAwareEmbeddingModel {

  private static final String DEFAULT_GOOGLEAPIS_ENDPOINT_SUFFIX = "-aiplatform.googleapis.com:443";
  private static final int COMPUTE_TOKENS_MAX_INPUTS_PER_REQUEST = 2048;
  private static final int DEFAULT_MAX_SEGMENTS_PER_BATCH = 250;
  private static final int DEFAULT_MAX_TOKENS_PER_BATCH = 20000;
  private final PredictionServiceSettings settings;
  private final LlmUtilityServiceSettings llmUtilitySettings;
  private final EndpointName endpointName;
  private final Integer maxRetries;
  private final Integer maxSegmentsPerBatch;
  private final Integer maxTokensPerBatch;
  private final VertexAiEmbeddingModel.TaskType taskType;
  private final String titleMetadataKey;
  private final Integer outputDimensionality;
  private final Boolean autoTruncate;

  public VertexAiEmbeddingModel(String endpoint, String project, Credentials credentials, String location, String publisher,
                                String modelName, Integer maxRetries, Integer maxSegmentsPerBatch, Integer maxTokensPerBatch,
                                VertexAiEmbeddingModel.TaskType taskType, String titleMetadataKey, Integer outputDimensionality,
                                Boolean autoTruncate) {

    String regionWithBaseAPI = endpoint != null ? endpoint :
        ValidationUtils.ensureNotBlank(location, "location") + "-aiplatform.googleapis.com:443";

    this.endpointName = EndpointName.ofProjectLocationPublisherModelName(
        ValidationUtils.ensureNotBlank(project, "project"),
        location,
        ValidationUtils.ensureNotBlank(publisher, "publisher"),
        ValidationUtils.ensureNotBlank(modelName, "modelName"));

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

    this.maxRetries = (Integer)Utils.getOrDefault(maxRetries, 3);
    this.maxSegmentsPerBatch = ValidationUtils.ensureGreaterThanZero((Integer)Utils.getOrDefault(maxSegmentsPerBatch, 250), "maxSegmentsPerBatch");
    this.maxTokensPerBatch = ValidationUtils.ensureGreaterThanZero((Integer)Utils.getOrDefault(maxTokensPerBatch, 20000), "maxTokensPerBatch");
    this.taskType = taskType;
    this.titleMetadataKey = (String)Utils.getOrDefault(titleMetadataKey, "title");
    this.outputDimensionality = outputDimensionality;
    this.autoTruncate = (Boolean)Utils.getOrDefault(autoTruncate, false);
  }

  public Response<List<Embedding>> embedAll(List<TextSegment> segments) {

    try {
      PredictionServiceClient client = PredictionServiceClient.create(this.settings);

      Response response;
      try {
        List<Embedding> embeddings = new ArrayList();
        int inputTokenCount = 0;
        List<Integer> tokensCounts = this.calculateTokensCounts(segments);
        List<Integer> batchSizes = this.groupByBatches(tokensCounts);
        int i = 0;
        int j = 0;

        while(true) {
          if (i >= segments.size() || j >= batchSizes.size()) {
            response = Response.from(embeddings, new TokenUsage(inputTokenCount));
            break;
          }

          List<TextSegment> batch = segments.subList(i, i + (Integer)batchSizes.get(j));
          List<Value> instances = new ArrayList();
          Iterator batchIterator = batch.iterator();

          while(batchIterator.hasNext()) {
            TextSegment segment = (TextSegment)batchIterator.next();
            VertexAiEmbeddingInstance embeddingInstance = new VertexAiEmbeddingInstance(segment.text());
            if (this.taskType != null) {
              embeddingInstance.setTaskType(this.taskType);
              if (this.taskType.equals(VertexAiEmbeddingModel.TaskType.RETRIEVAL_DOCUMENT)) {
                embeddingInstance.setTitle(segment.metadata().getString(this.titleMetadataKey));
              }
            }

            Value.Builder instanceBuilder = Value.newBuilder();
            JsonFormat.parser().merge(Json.toJson(embeddingInstance), instanceBuilder);
            instances.add(instanceBuilder.build());
          }

          VertexAiEmbeddingParameters parameters = new VertexAiEmbeddingParameters(this.outputDimensionality, (Boolean)Utils.getOrDefault(this.autoTruncate, false));
          Value.Builder parameterBuilder = Value.newBuilder();
          JsonFormat.parser().merge(Json.toJson(parameters), parameterBuilder);
          PredictResponse predictResponse = (PredictResponse)RetryUtils.withRetry(() -> {
            return client.predict(this.endpointName, instances, parameterBuilder.build());
          }, this.maxRetries);
          embeddings.addAll((Collection)predictResponse.getPredictionsList().stream().map(
              VertexAiEmbeddingModel::toEmbedding).collect(Collectors.toList()));

          Value prediction;
          for(Iterator predictionsIterator = predictResponse.getPredictionsList().iterator(); predictionsIterator.hasNext(); inputTokenCount += extractTokenCount(prediction)) {
            prediction = (Value)predictionsIterator.next();
          }

          i += (Integer)batchSizes.get(j);
          ++j;
        }
      } catch (Throwable throwable) {
        if (client != null) {
          try {
            client.close();
          } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
          }
        }

        throw throwable;
      }

      if (client != null) {
        client.close();
      }

      return response;
    } catch (Exception var18) {
      Exception e = var18;
      throw new RuntimeException(e);
    }
  }

  public List<Integer> calculateTokensCounts(List<TextSegment> segments) {

    try {
      LlmUtilityServiceClient utilClient = LlmUtilityServiceClient.create(this.llmUtilitySettings);

      ArrayList tokenCounts;
      try {
        ArrayList<Integer> incrementaltokensCounts = new ArrayList();
        int i = 0;

        while(true) {
          if (i >= segments.size()) {
            tokenCounts = incrementaltokensCounts;
            break;
          }

          List<TextSegment> batch = segments.subList(i, Math.min(i + 2048, segments.size()));
          List<Value> instances = new ArrayList();
          Iterator batchIterator = batch.iterator();

          while(batchIterator.hasNext()) {
            TextSegment segment = (TextSegment)batchIterator.next();
            Value.Builder instanceBuilder = Value.newBuilder();
            JsonFormat.parser().merge(Json.toJson(new VertexAiEmbeddingInstance(segment.text())), instanceBuilder);
            instances.add(instanceBuilder.build());
          }

          ComputeTokensRequest computeTokensRequest = ComputeTokensRequest.newBuilder().setEndpoint(this.endpointName.toString()).addAllInstances(instances).build();
          ComputeTokensResponse computeTokensResponse = utilClient.computeTokens(computeTokensRequest);
          incrementaltokensCounts.addAll((Collection)computeTokensResponse.getTokensInfoList().stream().map(TokensInfo::getTokensCount).collect(Collectors.toList()));
          i += 2048;
        }
      } catch (Throwable var11) {
        if (utilClient != null) {
          try {
            utilClient.close();
          } catch (Throwable var10) {
            var11.addSuppressed(var10);
          }
        }

        throw var11;
      }

      if (utilClient != null) {
        utilClient.close();
      }

      return tokenCounts;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Integer knownDimension() {
    return VertexAiEmbeddingModelName.knownDimension(this.endpointName.getModel());
  }

  private List<Integer> groupByBatches(List<Integer> tokensCounts) {
    List<List<Integer>> batches = new ArrayList();
    List<Integer> currentBatch = new ArrayList();
    int currentBatchSum = 0;
    Iterator var5 = tokensCounts.iterator();

    while(true) {
      while(var5.hasNext()) {
        Integer tokensCount = (Integer)var5.next();
        if (currentBatchSum + tokensCount <= this.maxTokensPerBatch && currentBatch.size() < this.maxSegmentsPerBatch) {
          currentBatch.add(tokensCount);
          currentBatchSum += tokensCount;
        } else {
          batches.add(currentBatch);
          currentBatch = new ArrayList();
          currentBatch.add(tokensCount);
          currentBatchSum = tokensCount;
        }
      }

      if (!currentBatch.isEmpty()) {
        batches.add(currentBatch);
      }

      return (List)batches.stream().mapToInt(List::size).boxed().collect(Collectors.toList());
    }
  }

  private static Embedding toEmbedding(Value prediction) {
    List<Float> vector = (List)((Value)prediction.getStructValue().getFieldsMap().get("embeddings")).getStructValue().getFieldsOrThrow("values").getListValue().getValuesList().stream().map((v) -> {
      return (float)v.getNumberValue();
    }).collect(Collectors.toList());
    return Embedding.from(vector);
  }

  private static int extractTokenCount(Value prediction) {
    return (int)((Value)((Value)((Value)prediction.getStructValue().getFieldsMap().get("embeddings")).getStructValue().getFieldsMap().get("statistics")).getStructValue().getFieldsMap().get("token_count")).getNumberValue();
  }

  public static VertexAiEmbeddingModel.Builder builder() {
    Iterator var0 = ServiceHelper.loadFactories(VertexAiEmbeddingModelBuilderFactory.class).iterator();
    if (var0.hasNext()) {
      VertexAiEmbeddingModelBuilderFactory factory = (VertexAiEmbeddingModelBuilderFactory)var0.next();
      return (VertexAiEmbeddingModel.Builder)factory.get();
    } else {
      return new VertexAiEmbeddingModel.Builder();
    }
  }

  public static enum TaskType {
    RETRIEVAL_QUERY,
    RETRIEVAL_DOCUMENT,
    SEMANTIC_SIMILARITY,
    CLASSIFICATION,
    CLUSTERING,
    QUESTION_ANSWERING,
    FACT_VERIFICATION,
    CODE_RETRIEVAL_QUERY;

    private TaskType() {
    }
  }

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
    private VertexAiEmbeddingModel.TaskType taskType;
    private String titleMetadataKey;
    private Integer outputDimensionality;
    private Boolean autoTruncate;

    public Builder() {
    }

    public VertexAiEmbeddingModel.Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public VertexAiEmbeddingModel.Builder project(String project) {
      this.project = project;
      return this;
    }

    public VertexAiEmbeddingModel.Builder credentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public VertexAiEmbeddingModel.Builder location(String location) {
      this.location = location;
      return this;
    }

    public VertexAiEmbeddingModel.Builder publisher(String publisher) {
      this.publisher = publisher;
      return this;
    }

    public VertexAiEmbeddingModel.Builder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public VertexAiEmbeddingModel.Builder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public VertexAiEmbeddingModel.Builder maxSegmentsPerBatch(Integer maxBatchSize) {
      this.maxSegmentsPerBatch = maxBatchSize;
      return this;
    }

    public VertexAiEmbeddingModel.Builder maxTokensPerBatch(Integer maxTokensPerBatch) {
      this.maxTokensPerBatch = maxTokensPerBatch;
      return this;
    }

    public VertexAiEmbeddingModel.Builder taskType(
        VertexAiEmbeddingModel.TaskType taskType) {
      this.taskType = taskType;
      return this;
    }

    public VertexAiEmbeddingModel.Builder titleMetadataKey(String titleMetadataKey) {
      this.titleMetadataKey = titleMetadataKey;
      return this;
    }

    public VertexAiEmbeddingModel.Builder autoTruncate(Boolean autoTruncate) {
      this.autoTruncate = autoTruncate;
      return this;
    }

    public VertexAiEmbeddingModel.Builder outputDimensionality(Integer outputDimensionality) {
      this.outputDimensionality = outputDimensionality;
      return this;
    }

    public VertexAiEmbeddingModel build() {
      return new VertexAiEmbeddingModel(this.endpoint, this.project, this.credentials, this.location, this.publisher, this.modelName, this.maxRetries, this.maxSegmentsPerBatch, this.maxTokensPerBatch, this.taskType, this.titleMetadataKey, this.outputDimensionality, this.autoTruncate);
    }
  }

  class VertexAiEmbeddingInstance {
    private String content;
    private String title;
    private VertexAiEmbeddingModel.TaskType task_type;

    VertexAiEmbeddingInstance(String content) {
      this.content = content;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public void setTaskType(VertexAiEmbeddingModel.TaskType taskType) {
      this.task_type = taskType;
    }
  }

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

  interface VertexAiEmbeddingModelBuilderFactory  extends Supplier<Builder> {

  }
}
