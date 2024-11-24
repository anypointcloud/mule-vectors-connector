package org.mule.extension.vectors.internal.model.einstein;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class EinsteinEmbeddingModel extends DimensionAwareEmbeddingModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinEmbeddingModel.class);

  private static final Map<String, String> modelNameMapping = new HashMap<>();

  private static final String URL_BASE = "https://api.salesforce.com/einstein/platform/v1/models/";

  static {
    modelNameMapping.put("Anthropic Claude 3 Haiku on Amazon", "sfdc_ai__DefaultBedrockAnthropicClaude3Haiku");
    modelNameMapping.put("Azure OpenAI Ada 002", "sfdc_ai__DefaultAzureOpenAITextEmbeddingAda_002");
    modelNameMapping.put("Azure OpenAI GPT 3.5 Turbo", "sfdc_ai__DefaultAzureOpenAIGPT35Turbo");
    modelNameMapping.put("Azure OpenAI GPT 3.5 Turbo 16k", "sfdc_ai__DefaultAzureOpenAIGPT35Turbo_16k");
    modelNameMapping.put("Azure OpenAI GPT 4 Turbo", "sfdc_ai__DefaultAzureOpenAIGPT4Turbo");
    modelNameMapping.put("OpenAI Ada 002", "sfdc_ai__DefaultOpenAITextEmbeddingAda_002");
    modelNameMapping.put("OpenAI GPT 3.5 Turbo", "sfdc_ai__DefaultOpenAIGPT35Turbo");
    modelNameMapping.put("OpenAI GPT 3.5 Turbo 16k", "sfdc_ai__DefaultOpenAIGPT35Turbo_16k");
    modelNameMapping.put("OpenAI GPT 4", "sfdc_ai__DefaultOpenAIGPT4");
    modelNameMapping.put("OpenAI GPT 4 32k", "sfdc_ai__DefaultOpenAIGPT4_32k");
    modelNameMapping.put("OpenAI GPT 4o (Omni)", "sfdc_ai__DefaultOpenAIGPT4Omni");
    modelNameMapping.put("OpenAI GPT 4 Turbo", "sfdc_ai__DefaultOpenAIGPT4Turbo");
  }

  private final String modelName;

  private static String getModelName(String input) {
    return modelNameMapping.getOrDefault(input, "sfdc_ai__DefaultOpenAITextEmbeddingAda_002");
  }

  private final Integer dimensions;
  private final String accessToken;


  private EinsteinEmbeddingModel(String salesforceOrg, String clientId, String clientSecret, String modelName, Integer dimensions) {

    this.modelName = getModelName(modelName);
    this.dimensions = dimensions;
    this.accessToken = getAccessToken(salesforceOrg, clientId, clientSecret);
  }

  protected Integer knownDimension() {
    return this.dimensions != null ? this.dimensions : EinsteinEmbeddingModelName.knownDimension(this.modelName());
  }

  public String modelName() {
    return this.modelName;
  }

  public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {

    List<String> texts = (List<String>) textSegments.stream().map(TextSegment::text).collect(Collectors.toList());
    return this.embedTexts(texts);
  }

  private Response<List<Embedding>> embedTexts(List<String> texts) {

    List<Embedding> embeddings = new ArrayList<>();

    int tokenUsage = 0;

    // Loop through each array in batch of 16
    for(int x = 0; x < texts.size(); x += 16) {

      List<String> batch = texts.subList(x, Math.min(x + 16, texts.size()));

      String response = generateEmbeddings(buildPayload(batch));
      JSONObject jsonResponse = new JSONObject(response);

      tokenUsage += jsonResponse.getJSONObject("parameters")
          .getJSONObject("usage")
          .getInt("total_tokens");

      // Extract the 'embeddings' array
      JSONArray embeddingsArray = jsonResponse.getJSONArray("embeddings");

      // Loop through each embedding object in the embeddings array
      for (int i = 0; i < embeddingsArray.length(); i++) {
        // Extract the individual embedding object
        JSONObject embeddingObject = embeddingsArray.getJSONObject(i);

        // Get the 'embedding' array
        JSONArray embeddingArray = embeddingObject.getJSONArray("embedding");

        // Convert the 'embedding' JSONArray to a float array
        float[] vector = new float[embeddingArray.length()];
        for (int y = 0; y < embeddingArray.length(); y++) {
          vector[y] = (float) embeddingArray.getDouble(y); // Convert to float
        }

        // Create an Embedding object and add it to the list
        Embedding embedding = Embedding.from(vector);
        embeddings.add(embedding);
      }
    }

    return Response.from(embeddings, new TokenUsage(tokenUsage));
  }

  private String getAccessToken(String salesforceOrg, String clientId, String clientSecret) {

    String urlString = "https://" + salesforceOrg + ".my.salesforce.com/services/oauth2/token";
    String params = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

    try {
      URL url = new URL(urlString);

      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          // Parse JSON response and extract access_token
          JSONObject jsonResponse = new JSONObject(response.toString());
          return jsonResponse.getString("access_token");
        }
      } else {
        throw new ModuleException(
            "Error while getting access token for \"EINSTEIN\" embedding model service. Response code: " + responseCode,
            MuleVectorsErrorType.AI_SERVICES_FAILURE);
      }
    } catch (Exception e) {

      throw new ModuleException(
          "Error while getting access token for \"EINSTEIN\" embedding model service.",
          MuleVectorsErrorType.AI_SERVICES_FAILURE,
          e);
    }
  }

  private HttpURLConnection getConnectionObject(URL url, String accessToken) throws IOException {

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("x-sfdc-app-context", "EinsteinGPT");
    conn.setRequestProperty("x-client-feature-id", "ai-platform-models-connected-app");
    conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
    return conn;
  }

  private static String buildPayload(String text) {

    JSONArray input = new JSONArray();
    input.put(text);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("input", input);
    return jsonObject.toString();
  }

  private static String buildPayload(List<String> texts) {

    JSONArray input = new JSONArray(texts);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("input", input);
    return jsonObject.toString();
  }

  private String generateEmbeddings(String payload) {

    try {

      // Open connection
      String urlString = URL_BASE + getModelName(modelName) + "/embeddings";
      HttpURLConnection connection;
      try {
        URL url = new URL(urlString);
        connection = getConnectionObject(url, this.accessToken);
      } catch (Exception e) {
        throw new ModuleException(
            "Error while connecting to  \"EINSTEIN\" embedding model service.",
            MuleVectorsErrorType.AI_SERVICES_FAILURE,
            e);
      }

      // Write the request body to the OutputStream
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = payload.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);  // Writing the payload to the connection
      }

      // After writing, check the response code to ensure the request was successful
      int responseCode = connection.getResponseCode();

      // Only read the response if the request was successful
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          return response.toString();
        }
      } else {
        throw new ModuleException(
            "Error while generating embeddings with \"EINSTEIN\" embedding model service. Response code: " + responseCode,
            MuleVectorsErrorType.AI_SERVICES_FAILURE);

      }
    } catch (ModuleException e) {

      throw e;

    } catch (Exception e) {

      throw new ModuleException(
          "Error while generating embeddings with \"EINSTEIN\" embedding model service.",
          MuleVectorsErrorType.AI_SERVICES_FAILURE,
          e);
    }

  }

  public static EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder builder() {

    return new EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder();
  }

  public static class EinsteinEmbeddingModelBuilder {

    private String salesforceOrg;
    private String clientId;
    private String clientSecret;
    private String modelName;
    private Integer dimensions;
    private Tokenizer tokenizer;
    private HttpURLConnection connection;

    public EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder salesforceOrg(String salesforceOrg) {
      this.salesforceOrg = salesforceOrg;
      return this;
    }

    public EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder clientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder modelName(EinsteinEmbeddingModelName modelName) {
      this.modelName = modelName.toString();
      return this;
    }

    public EinsteinEmbeddingModel.EinsteinEmbeddingModelBuilder dimensions(Integer dimensions) {
      this.dimensions = dimensions;
      return this;
    }

    public EinsteinEmbeddingModelBuilder() {

    }

    public EinsteinEmbeddingModel build() {
      return new EinsteinEmbeddingModel(this.salesforceOrg, this.clientId, this.clientSecret, this.modelName, this.dimensions);
    }
  }
}
