package org.mule.extension.vectors.internal.connection.store;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

public interface BaseStoreConnection {

  String getVectorStore();

  void connect() throws ConnectionException;

  void disconnect();

  boolean isValid();
}
