package org.mule.extension.vectors.internal.connection.model.einstein;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EinsteinModelConnection implements BaseModelConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinModelConnection.class);

  private static final String URI_OAUTH_TOKEN = "/services/oauth2/token";
  private static final String PARAM_GRANT_TYPE = "grant_type";
  private static final String PARAM_CLIENT_ID = "client_id";
  private static final String PARAM_CLIENT_SECRET = "client_secret";
  private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
  private static final String EINSTEIN_PLATFORM_MODELS_URL = "https://api.salesforce.com/einstein/platform/v1/models/";

  private final String salesforceOrg;
  private final String clientId;
  private final String clientSecret;

  private String accessToken;

  public EinsteinModelConnection(String salesforceOrg, String clientId, String clientSecret) {

    this.salesforceOrg = salesforceOrg;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public String getEmbeddingModelService() {
    return Constants.EMBEDDING_MODEL_SERVICE_EINSTEIN;
  }

  @Override
  public void connect() throws ConnectionException {

    try {

      this.accessToken = getAccessToken(salesforceOrg, clientId, clientSecret);
      if (this.accessToken == null) {

        throw new ConnectionException("Failed to connect to Salesforce: HTTP " + accessToken);
      }
    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Salesforce", e);
    }
  }

  @Override
  public void disconnect() {

    // Add logic to invalidate connection
  }

  @Override
  public boolean isValid() {

    return isAccessTokenValid();
  }

  private  String getOAuthURL() {

    return Constants.URI_HTTPS_PREFIX + salesforceOrg + URI_OAUTH_TOKEN;
  }

  private String getOAuthParams() {

    return PARAM_GRANT_TYPE + "=" + GRANT_TYPE_CLIENT_CREDENTIALS
        + "&" + PARAM_CLIENT_ID + "=" + clientId
        + "&" + PARAM_CLIENT_SECRET + "=" + clientSecret;
  }

  /**
   * Authenticates with Salesforce and obtains an access token.
   *
   * @param salesforceOrg Salesforce organization identifier
   * @param clientId OAuth client ID
   * @param clientSecret OAuth client secret
   * @return Access token for API calls
   * @throws ModuleException if authentication fails
   */
  private String getAccessToken(String salesforceOrg, String clientId, String clientSecret) throws ConnectionException {

    String tokenUrl = getOAuthURL();
    String oAuthParams = getOAuthParams();

    try {
      URL url = new URL(tokenUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      // Configure connection for OAuth token request
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      // Write parameters to request body
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = oAuthParams.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        // Read and parse response
        try (java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          return new JSONObject(response.toString()).getString("access_token");
        }
      } else {
        throw new ConnectionException("Error while getting access token for \"EINSTEIN\" embedding model service. " +
                                          "Response code: " + responseCode);
      }
    } catch (ConnectionException e) {

      throw e;

    } catch (Exception e) {
      throw new ConnectionException(
          "Error while getting access token for \"EINSTEIN\" embedding model service.",
          e);
    }
  }

  private Boolean isAccessTokenValid() {

    String urlString = Constants.URI_HTTPS_PREFIX + salesforceOrg + "/services/oauth2/userinfo";

    try {
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "Bearer " + this.accessToken);

     return conn.getResponseCode() == HttpURLConnection.HTTP_OK;

    } catch (Exception e) {

      LOGGER.error("Error while validating access token for \"EINSTEIN\" embedding model service.", e);
      return false;
    }
  }

  /**
   * Creates and configures an HTTP connection for Einstein API requests.
   *
   * @param url The endpoint URL
   * @return Configured HttpURLConnection
   * @throws IOException if connection setup fails
   */
  private HttpURLConnection prepareHttpURLConnection(URL url) throws IOException, ConnectionException {

    if(accessToken == null) connect();
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setRequestProperty("x-sfdc-app-context", "EinsteinGPT");
    conn.setRequestProperty("x-client-feature-id", "ai-platform-models-connected-app");
    conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
    return conn;
  }

  /**
   * Builds JSON payload for batch text embedding request.
   *
   * @param texts List of texts to embed
   * @return JSON string payload
   */
  private String buildEmbeddingsPayload(List<String> texts) {

    JSONArray input = new JSONArray(texts);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("input", input);
    return jsonObject.toString();
  }

  /**
   * Makes the API call to Einstein to generate embeddings.
   *
   * @param inputs text list
   * @return JSON response string
   * @throws ModuleException if the API call fails
   */
  public String generateEmbeddings(List<String> inputs, String modelName) {

    return generateEmbeddings(inputs, modelName, false);
  }

  private String generateEmbeddings(List<String> inputs, String modelName, Boolean tokenExpired) {

    String payload = buildEmbeddingsPayload(inputs);

    int responseCode = -1;

    try {
      // Prepare connection
      String urlString = EINSTEIN_PLATFORM_MODELS_URL + modelName + "/embeddings";
      HttpURLConnection connection;
      try {

        URL url = new URL(urlString);
        connection = prepareHttpURLConnection(url);
      } catch (Exception e) {

        throw new ModuleException(
            "Error while connecting to  \"EINSTEIN\" embedding model service.",
            MuleVectorsErrorType.AI_SERVICES_FAILURE,
            e);
      }

      // Send request
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = payload.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        // Read response
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          return response.toString();
        }
      } else if (responseCode == 401 && !tokenExpired) {

        LOGGER.debug("Salesforce access token expired.");
        // Reconnect to get new token
        connect();
        // Re-try one more time
        return generateEmbeddings(inputs, modelName, true);

      } else {

        // Read error response
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
          String inputLine;
          StringBuilder response = new StringBuilder();
          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }
          // Print the error response
          LOGGER.error("Error (HTTP " + responseCode + "): " + response.toString());
          throw new ModuleException(
              String.format("Error while generating embeddings with \"EINSTEIN\" embedding model service. Response code: %s. Response %s.",
                            responseCode,
                            response.toString()),
              MuleVectorsErrorType.AI_SERVICES_FAILURE);
        }
      }
    } catch (ModuleException e) {

      throw e;
    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while generating embeddings with \"EINSTEIN\" embedding model service. Response code: %s", responseCode),
          MuleVectorsErrorType.AI_SERVICES_FAILURE,
          e);
    }
  }
}
