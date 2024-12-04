package org.mule.extension.vectors.internal.connection.model.openai;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

@Alias("openAI")
@DisplayName("OpenAI")
public class OpenAIModelConnection implements BaseModelConnection {

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

  }

  @Override
  public void disconnect() {

  }

  @Override
  public boolean isValid() {
    return false;
  }
}
