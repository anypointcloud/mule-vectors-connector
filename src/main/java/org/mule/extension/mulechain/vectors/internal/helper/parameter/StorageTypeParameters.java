package org.mule.extension.mulechain.vectors.internal.helper.parameter;

import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.provider.StorageTypeProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class StorageTypeParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(StorageTypeProvider.class)
  @Optional(defaultValue = Constants.STORAGE_TYPE_LOCAL)
  private String storageType;

  public String getStorageType() {
    return storageType;
  }

}
