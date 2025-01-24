package org.mule.extension.vectors.internal.model.multimodal.vertexai;

import com.google.auth.Credentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.mule.extension.vectors.internal.model.multimodal.EmbeddingMultimodalModel;
import org.mule.extension.vectors.internal.model.text.vertexai.VertexAiEmbeddingModelName;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Json.toJson;
import static dev.langchain4j.internal.RetryUtils.withRetry;

/**
 * Implementation of the EmbeddingMultimodalModel interface for Vertex AI.
 */
public class VertexAiEmbeddingMultimodalModel implements EmbeddingMultimodalModel {

  private final PredictionServiceClient client;
  private final EndpointName endpointName;
  private final Integer maxRetries;

  // Constructor that handles both cases: with or without an existing PredictionServiceClient
  public VertexAiEmbeddingMultimodalModel(PredictionServiceClient client, Credentials credentials, String endpoint, String project,
                                          String location, String publisher, String modelName, Integer maxRetries) {

    this.endpointName = EndpointName.ofProjectLocationPublisherModelName(project, location, publisher, modelName);
    if (client != null) {
      this.client = client;  // Use provided client
    } else {
      // Create client using credentials if none provided
      try {
        String regionWithBaseAPI = endpoint != null ? endpoint : location + "-aiplatform.googleapis.com:443";
        this.client = PredictionServiceClient.create(
            PredictionServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .setEndpoint(regionWithBaseAPI)
                .build()
        );
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize Vertex AI settings", e);
      }
    }
    this.maxRetries = maxRetries != null ? maxRetries : 3;
  }

  public Integer dimension() {
    return VertexAiEmbeddingMultimodalModelName.knownDimension(this.endpointName.getModel());
  }

  @Override
  public Response<Embedding> embedText(String text) {
    return embedSingle(new VertexAiMultimodalInput(text, null));
  }

  @Override
  public Response<Embedding> embedImage(byte[] imageBytes) {
    return embedSingle(new VertexAiMultimodalInput(null, imageBytes));
  }

  @Override
  public Response<Embedding> embedTextAndImage(String text, byte[] imageBytes) {
    return embedSingle(new VertexAiMultimodalInput(text, imageBytes));
  }

  @Override
  public Response<List<Embedding>> embedTexts(List<String> texts) {
    List<VertexAiMultimodalInput> inputs = texts.stream()
        .map(text -> new VertexAiMultimodalInput(text, null))
        .collect(Collectors.toList());
    return embedBatch(inputs);
  }

  @Override
  public Response<List<Embedding>> embedImages(List<byte[]> imageBytesList) {
    List<VertexAiMultimodalInput> inputs = imageBytesList.stream()
        .map(bytes -> new VertexAiMultimodalInput(null, bytes))
        .collect(Collectors.toList());
    return embedBatch(inputs);
  }

  private Response<Embedding> embedSingle(VertexAiMultimodalInput input) {
    try {
      Value.Builder instanceBuilder = Value.newBuilder();
      JsonFormat.parser().merge(toJson(input), instanceBuilder);

      Value parameter = Value.newBuilder().build(); // Adjust if additional parameters are needed.

      PredictResponse response = withRetry(() -> client.predict(endpointName, Arrays.asList(instanceBuilder.build()), parameter), maxRetries);

      Embedding embedding = response.getPredictionsList().stream()
          .map(VertexAiEmbeddingMultimodalModel::toEmbedding)
          .findFirst()
          .orElseThrow(() -> new RuntimeException("No embedding found in response"));

      return Response.from(embedding);
    } catch (Exception e) {
      throw new RuntimeException("Error during embedding generation", e);
    }
  }

  private Response<List<Embedding>> embedBatch(List<VertexAiMultimodalInput> inputs) {
    try {
      List<Value> instances = inputs.stream()
          .map(input -> {
            Value.Builder instanceBuilder = Value.newBuilder();
            try {
              JsonFormat.parser().merge(toJson(input), instanceBuilder);
            } catch (IOException e) {
              throw new RuntimeException("Error serializing input", e);
            }
            return instanceBuilder.build();
          })
          .collect(Collectors.toList());

      Value parameter = Value.newBuilder().build(); // Adjust if additional parameters are needed.

      PredictResponse response = withRetry(() -> client.predict(endpointName, instances, parameter), maxRetries);

      List<Embedding> embeddings = response.getPredictionsList().stream()
          .map(VertexAiEmbeddingMultimodalModel::toEmbedding)
          .collect(Collectors.toList());

      return Response.from(embeddings);
    } catch (Exception e) {
      throw new RuntimeException("Error during batch embedding generation", e);
    }
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
        .collect(Collectors.toList());

    return Embedding.from(vector);
  }

  /**
   * Helper class for representing multimodal inputs.
   */
  private static class VertexAiMultimodalInput {
    private final String text;
    private final byte[] image;

    public VertexAiMultimodalInput(String text, byte[] image) {
      this.text = text;
      this.image = image;
    }

    public String getText() {
      return text;
    }

    public byte[] getImage() {
      return image;
    }
  }

  public static class Builder {
    private PredictionServiceClient client;
    private Credentials credentials;
    private String endpoint;
    private String project;
    private String location;
    private String publisher;
    private String modelName;
    private Integer maxRetries = 3;

    public Builder client(PredictionServiceClient client) {
      this.client = client;
      return this;
    }

    public Builder credentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder project(String project) {
      this.project = project;
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

    public VertexAiEmbeddingMultimodalModel build() {
      return new VertexAiEmbeddingMultimodalModel(
          client, credentials, endpoint, project,
          location, publisher, modelName, maxRetries
      );
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
