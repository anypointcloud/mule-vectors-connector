package org.mule.extension.vectors.internal.connection.storage;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

public interface BaseStorageConnection {

  String getStorageType();

  void connect() throws ConnectionException;

  void disconnect();

  boolean isValid();
}
