package org.mule.extension.vectors.internal.connection.store.pgvector;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class PGVectorStoreConnection implements BaseStoreConnection {

  private String host;
  private int port;
  private String database;
  private String userName;
  private String password;

  public PGVectorStoreConnection(String host, int port, String database, String userName, String password) {
    this.host = host;
    this.port = port;
    this.database = database;
    this.userName = userName;
    this.password = password;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getDatabase() {
    return database;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_PGVECTOR;
  }
}
