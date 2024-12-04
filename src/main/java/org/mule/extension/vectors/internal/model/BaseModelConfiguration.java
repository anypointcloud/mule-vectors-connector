package org.mule.extension.vectors.internal.model;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;

public interface BaseModelConfiguration {

  BaseModelConnection getConnection();
}
