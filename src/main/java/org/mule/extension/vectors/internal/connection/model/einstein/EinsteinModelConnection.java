package org.mule.extension.vectors.internal.connection.model.einstein;

import org.json.JSONObject;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EinsteinModelConnection implements BaseModelConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinModelConnection.class);

  public static final String URI_OAUTH_TOKEN = "/services/oauth2/token";
  public static final String QUERY_PARAM_GRANT_TYPE = "grant_type";
  public static final String QUERY_PARAM_CLIENT_ID = "client_id";
  public static final String QUERY_PARAM_CLIENT_SECRET = "client_secret";
  public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

  private final String salesforceOrg;
  private final String clientId;
  private final String clientSecret;

  private String accessToken;

  public String getAccessToken() {
    return accessToken;
  }

  public EinsteinModelConnection(String salesforceOrg, String clientId, String clientSecret) {

    this.salesforceOrg = salesforceOrg;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public String getSalesforceOrg() {
    return salesforceOrg;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
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
    return true;
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

    String urlString = Constants.URI_HTTPS_PREFIX + salesforceOrg + "/services/oauth2/token";
    String params = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

    try {
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      // Configure connection for OAuth token request
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      // Write parameters to request body
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
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

  public static String getOAuthURL(String salesforceOrg) {
    return Constants.URI_HTTPS_PREFIX + salesforceOrg + URI_OAUTH_TOKEN;
  }

  public static String getOAuthParams(String clientId, String clientSecret) {
    return QUERY_PARAM_GRANT_TYPE + "=" + GRANT_TYPE_CLIENT_CREDENTIALS
        + "&" + QUERY_PARAM_CLIENT_ID + "=" + clientId
        + "&" + QUERY_PARAM_CLIENT_SECRET + "=" + clientSecret;
  }
}
