package org.mule.extension.vectors.internal.connection.store.elasticsearch;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class ElasticsearchStoreConnection implements BaseStoreConnection {

  private String url;
  private String userName;
  private String password;

  public ElasticsearchStoreConnection(String url, String userName, String password) {
    this.url = url;
    this.userName = userName;
    this.password = password;
  }

  public String getUrl() {
    return url;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_ELASTICSEARCH;
  }
}
