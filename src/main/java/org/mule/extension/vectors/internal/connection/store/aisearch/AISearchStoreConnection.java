package org.mule.extension.vectors.internal.connection.store.aisearch;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class AISearchStoreConnection implements BaseStoreConnection {

  private String url;
  private String apiKey;

  public AISearchStoreConnection(String url, String apiKey) {
    this.url = url;
    this.apiKey = apiKey;
  }

  public String getUrl() {
    return url;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_AI_SEARCH;
  }

  @Override
  public void connect() {

  }

  @Override
  public void disconnect() {

  }

  @Override
  public boolean isValid() {
    return false;
  }
}
