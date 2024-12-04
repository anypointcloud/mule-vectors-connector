package org.mule.extension.vectors.internal.connection.store.milvus;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class MilvusStoreConnection implements BaseStoreConnection {

  private String url;

  public MilvusStoreConnection(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String getVectorStore() {

    return Constants.VECTOR_STORE_MILVUS;
  }
}
