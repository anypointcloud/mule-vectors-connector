package org.mule.extension.vectors.internal.connection.model.einstein;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

  private String salesforceOrg;
  private String clientId;
  private String clientSecret;

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
      int responseCode = getConnectionResponseCode(salesforceOrg, clientId, clientSecret);
      if (responseCode != 200) {

        throw new ConnectionException("Failed to connect to Salesforce: HTTP " + responseCode);
      }
    } catch (IOException e) {

      throw new ConnectionException("Failed to connect to Salesforce", e);
    }
  }

  @Override
  public void disconnect() {

    // Add logic to invalidate connection
  }

  @Override
  public boolean isValid() {
    return false;
  }

  private int getConnectionResponseCode(String salesforceOrg, String clientId, String clientSecret) throws IOException {

    LOGGER.debug("Preparing request for connection for salesforce org:{}", salesforceOrg);

    String urlStr = getOAuthURL(salesforceOrg);
    String urlParameters = getOAuthParams(clientId, clientSecret);

    byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod(Constants.HTTP_METHOD_POST);
    conn.getOutputStream().write(postData);
    int respCode = conn.getResponseCode();

    LOGGER.debug("Response code for connection request:{}", respCode);
    return respCode;
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
