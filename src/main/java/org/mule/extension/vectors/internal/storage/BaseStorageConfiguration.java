package org.mule.extension.vectors.internal.storage;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;

public interface BaseStorageConfiguration {

  BaseStorageConnection getConnection();
}
