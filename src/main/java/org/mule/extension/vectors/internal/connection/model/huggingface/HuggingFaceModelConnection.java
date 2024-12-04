package org.mule.extension.vectors.internal.connection.model.huggingface;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

@Alias("huggingFace")
@DisplayName("Hugging Face")
public class HuggingFaceModelConnection implements BaseModelConnection {

  private String apiKey;

  public HuggingFaceModelConnection(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public String getEmbeddingModelService() {
    return Constants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE;
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
