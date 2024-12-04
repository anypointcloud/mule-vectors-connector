package org.mule.extension.vectors.internal.connection.store.pinecone;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class PineconeStoreConnection implements BaseStoreConnection {

  private String cloud;
  private String region;
  private String apiKey;

  public PineconeStoreConnection(String cloud, String region, String apiKey) {
    this.cloud = cloud;
    this.region = region;
    this.apiKey = apiKey;
  }

  public String getCloud() {
    return cloud;
  }

  public String getRegion() {
    return region;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_PINECONE;
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
