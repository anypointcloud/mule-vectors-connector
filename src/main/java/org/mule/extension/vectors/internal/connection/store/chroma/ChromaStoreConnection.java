package org.mule.extension.vectors.internal.connection.store.chroma;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChromaStoreConnection implements BaseStoreConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChromaStoreConnection.class);

  private static final String API_ENDPOINT = "/api/v1";

  private String url;

  public ChromaStoreConnection(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_CHROMA;
  }

  @Override
  public void connect() throws ConnectionException {

    try {

      doHttpRequest();
    } catch (ConnectionException e) {

      throw e;

    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Chroma.", e);
    }
  }

  @Override
  public void disconnect() {

  }

  @Override
  public boolean isValid() {

    try {

      doHttpRequest();
      return true;

    } catch (Exception e) {

      LOGGER.error("Failed to validate connection to Chroma.", e);
      return false;
    }
  }

  private void doHttpRequest() throws ConnectionException {

    try {

      // Create URL object
      URL healthUrl = new URL(url + API_ENDPOINT);
      HttpURLConnection connection = (HttpURLConnection) healthUrl.openConnection();

      // Set request method to GET
      connection.setRequestMethod("GET");

      // Set headers
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");

      // Get the response code
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        // Read error response
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
          String inputLine;
          StringBuilder response = new StringBuilder();
          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }
          // Print the error response
          LOGGER.error("Error (HTTP " + responseCode + "): " + response.toString());
          throw new ConnectionException("Impossible to connect to Chroma. " + "Error (HTTP " + responseCode + "): " + response.toString());
        }
      }

    } catch (ConnectionException e) {

      throw e;

    } catch (Exception e) {

      LOGGER.error("Impossible to connect to Chroma", e);
      throw new ConnectionException("Impossible to connect to Chroma", e);
    }
  }
}
