package org.mule.extension.vectors.internal.model.multimodal.vertexai;

import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.auth.Credentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.mule.extension.vectors.internal.model.multimodal.EmbeddingMultimodalModel;
import org.mule.extension.vectors.internal.model.text.vertexai.VertexAiEmbeddingModelName;
import org.mule.extension.vectors.internal.operation.EmbeddingOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.langchain4j.internal.Json.toJson;
import static dev.langchain4j.internal.RetryUtils.withRetry;

/**
 * Implementation of the EmbeddingMultimodalModel interface for Vertex AI.
 */
public class VertexAiEmbeddingMultimodalModel implements EmbeddingMultimodalModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(VertexAiEmbeddingMultimodalModel.class);

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

        // Configure custom retry settings
        RetrySettings retrySettings = RetrySettings.newBuilder()
            .setMaxAttempts(3) // Maximum number of retries
            .setInitialRetryDelay(Duration.ofMillis(500)) // Initial retry delay
            .setRetryDelayMultiplier(1.5) // Multiplier for subsequent retries
            .setMaxRetryDelay(Duration.ofMillis(5000)) // Maximum retry delay
            .setTotalTimeout(Duration.ofMillis(60000)) // Total timeout for the operation
            .build();

        // Customize the predict settings
        PredictionServiceSettings.Builder settingsBuilder = PredictionServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .setEndpoint(regionWithBaseAPI);

        // Apply retry settings to the predictSettings
        settingsBuilder
            .predictSettings()
            .setRetrySettings(retrySettings);

        // Build the PredictionServiceClient
        this.client = PredictionServiceClient.create(settingsBuilder.build());

      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize Vertex AI settings", e);
      }
    }
    this.maxRetries = maxRetries != null ? maxRetries : 3;
  }

  @Override
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
      // Log input details for debugging
      LOGGER.debug("Multimodal Input - Text Length: {}, Image Bytes: {}",
                   input.getText() != null ? input.getText().length() : "null",
                   input.getImage() != null ? input.getImage().length : "null");

      List<Value> instances = new ArrayList<>();
      instances.add(input.instanceValue());

      Value parameterValue = Value.newBuilder().build();

      // Validate input size before making API call
      validateInputSize(input);

      PredictResponse response = withRetry(() ->
           client.predict(endpointName, instances, parameterValue), maxRetries);

      // LOGGER.debug("Predict Response " + response);

      Embedding embedding = response.getPredictionsList().stream()
          .map(VertexAiEmbeddingMultimodalModel::toEmbedding)
          .findFirst()
          .orElseThrow(() -> new RuntimeException("No embedding found in response"));

      return Response.from(embedding);

    } catch (Exception e) {
      LOGGER.error("Embedding generation failed", e);
      throw new RuntimeException("Error during embedding generation", e);
    }
  }

  private void validateInputSize(VertexAiMultimodalInput input) {

    int MAX_IMAGE_SIZE = 20 * 1024 * 1024;  // 5MB

    if (input.getImage() != null && input.getImage().length > MAX_IMAGE_SIZE) {
      throw new IllegalArgumentException("Image input exceeds maximum allowed size");
    }
  }

  private Response<List<Embedding>> embedBatch(List<VertexAiMultimodalInput> inputs) {
    try {
      List<Value> instances = inputs.stream()
          .map(input -> { return input.instanceValue(); })
          .collect(Collectors.toList());

      Value parameterValue = Value.newBuilder().build(); // Adjust if additional parameters are needed.

      PredictResponse response = withRetry(() -> client.predict(endpointName, instances, parameterValue), maxRetries);

      //LOGGER.debug("Predict Response " + response);

      List<Embedding> embeddings = response.getPredictionsList().stream()
          .map(VertexAiEmbeddingMultimodalModel::toEmbedding)
          .collect(Collectors.toList());

      return Response.from(embeddings);
    } catch (Exception e) {
      throw new RuntimeException("Error during batch embedding generation", e);
    }
  }

  private static Embedding toEmbedding(Value prediction) {

    List<Float> vector = null;

    if (prediction.getStructValue().containsFields("textEmbedding")) {

      LOGGER.debug ("Processing textEmbedding");
      Value textEmbedding = prediction.getStructValue().getFieldsOrThrow("textEmbedding");
      float[] textVector = toVector(textEmbedding);
      vector = IntStream.range(0, textVector.length)
          .mapToObj(i -> textVector[i])
          .collect(Collectors.toList());
    }

    if (prediction.getStructValue().containsFields("imageEmbedding")) {

      LOGGER.debug ("Processing imageEmbedding");
      Value imageEmbedding = prediction.getStructValue().getFieldsOrThrow("imageEmbedding");
      float[] imageVector = toVector(imageEmbedding);
      vector = IntStream.range(0, imageVector.length)
          .mapToObj(i -> imageVector[i])
          .collect(Collectors.toList());
    }

    if (prediction.getStructValue().containsFields("videoEmbeddings")) {

      LOGGER.debug ("Processing videoEmbeddings");
      Value videoEmbeddings = prediction.getStructValue().getFieldsOrThrow("videoEmbeddings");
      if (videoEmbeddings.getListValue().getValues(0).getStructValue().containsFields("embedding")) {
        Value embeddings = videoEmbeddings.getListValue()
            .getValues(0)
            .getStructValue()
            .getFieldsOrThrow("embedding");
        float[] videoVector = toVector(embeddings);
        vector = IntStream.range(0, videoVector.length)
            .mapToObj(i -> videoVector[i])
            .collect(Collectors.toList());
      }
    }

    return Embedding.from(vector);
  }

  private static float[] toVector(Value value) {

    float[] floats = new float[value.getListValue().getValuesList().size()];
    int index = 0;
    for (Value v : value.getListValue().getValuesList()) {
      double d = v.getNumberValue();
      floats[index++] = Double.valueOf(d).floatValue();
    }
    return floats;
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

    public Value instanceValue() {

      Value instanceValue;

      try {
        // Convert the image to Base64
        byte[] imageData = Base64.getEncoder().encode(image);
        String encodedImage = new String(imageData, StandardCharsets.UTF_8);

        Value.Builder instanceBuilder = Value.newBuilder();

        JsonObject jsonInstance = new JsonObject();
        if(text != null) {

          jsonInstance.addProperty("text", text);
        }
        if(image != null) {

          JsonObject jsonImage = new JsonObject();
          jsonImage.addProperty("bytesBase64Encoded", encodedImage);
          jsonInstance.add("image", jsonImage);
        }

        Value.Builder builder = Value.newBuilder();
        JsonFormat.parser().merge(jsonInstance.toString(), builder);
        instanceValue =  builder.build();

      } catch (IOException e) {
        throw new RuntimeException("Error serializing input", e);
      }

      return instanceValue;
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
