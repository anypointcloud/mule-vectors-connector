package org.mule.extension.vectors.internal.store;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;

public interface BaseStoreConfiguration {

  BaseStoreConnection getConnection();
}
