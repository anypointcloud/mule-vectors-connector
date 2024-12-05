package org.mule.extension.vectors.internal.connection.model.openai;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Alias("openAI")
@DisplayName("OpenAI")
public class OpenAIModelConnection implements BaseModelConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIModelConnection.class);

  private static final String ENDPOINT = "https://api.openai.com/v1/models";

  private String apiKey;

  public OpenAIModelConnection(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public String getEmbeddingModelService() {
    return Constants.EMBEDDING_MODEL_SERVICE_OPENAI;
  }

  @Override
  public void connect() throws ConnectionException {

    if(apiKey.compareTo("demo") != 0) {

      try {

        doAuthenticatedHttpRequest();
      } catch (ConnectionException e) {

        throw e;

      } catch (Exception e) {

        throw new ConnectionException("Failed to connect to Open AI.", e);
      }
    }
  }

  @Override
  public void disconnect() {

  }

  @Override
  public boolean isValid() {

    try {

      if(apiKey.compareTo("demo") != 0) doAuthenticatedHttpRequest();
      return true;

    } catch (Exception e) {

      LOGGER.error("Failed to validate connection to Open AI.", e);
      return false;
    }
  }

  private void doAuthenticatedHttpRequest() throws ConnectionException {

    try {
      // Create the URL object
      URL url = new URL(ENDPOINT);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Set request method to GET
      connection.setRequestMethod("GET");

      // Set headers
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);

      // Check response code
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        // Read the error response
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        // Print the error response
        LOGGER.error("Error (HTTP " + responseCode + "): " + response.toString());
        throw new ConnectionException("Impossible to connect to OpenAI. " + "Error (HTTP " + responseCode + "): " + response.toString());
      }

    } catch (ConnectionException e) {

      throw e;

    } catch (Exception e) {

      LOGGER.error("Impossible to connect to OpenAI", e);
      throw new ConnectionException("Impossible to connect to OpenAI", e);
    }
  }
}
