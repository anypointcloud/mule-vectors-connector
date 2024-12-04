package org.mule.extension.vectors.internal.connection.store.chroma;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class ChromaStoreConnection implements BaseStoreConnection {

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
