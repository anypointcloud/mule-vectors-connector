package com.mule.mulechain.vectors.internal;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class MuleChainVectorsFilterParameters {

  @Parameter
  @Optional
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(MuleChainVectorsMetadataKeyProvider.class)
  private String metadataKey;

  @Parameter
  @Optional
  @Expression(ExpressionSupport.SUPPORTED)
  private String metadataValue;

  public String metadataKey() {
    return metadataKey;
  }

  public String metadataValue() {
    return metadataValue;
  }

}
