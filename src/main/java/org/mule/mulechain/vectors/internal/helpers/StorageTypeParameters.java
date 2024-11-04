package org.mule.mulechain.vectors.internal.helpers;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class StorageTypeParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(StorageTypeEmbeddingProvider.class)
  @Optional(defaultValue = "Local")
  private String storageType;

  public String getStorageType() {
    return storageType;
  }

}
