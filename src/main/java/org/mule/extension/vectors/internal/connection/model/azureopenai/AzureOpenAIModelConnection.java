package org.mule.extension.vectors.internal.connection.model.azureopenai;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class AzureOpenAIModelConnection implements BaseModelConnection {

  private String endpoint;
  private String apiKey;

  public AzureOpenAIModelConnection(String endpoint, String apiKey) {
    this.endpoint = endpoint;
    this.apiKey = apiKey;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public String getEmbeddingModelService() {
    return Constants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI;
  }
}
