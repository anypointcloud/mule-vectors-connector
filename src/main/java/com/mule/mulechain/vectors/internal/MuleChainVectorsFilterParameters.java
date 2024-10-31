package com.mule.mulechain.vectors.internal;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class MuleChainVectorsFilterParameters {

  public static class SearchFilterParameters extends MuleChainVectorsFilterParameters{

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MuleChainVectorsMetadataKeyProvider.class)
    @Optional
    private String metadataKey;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional
    private String metadataValue;

    public String metadataKey() {
      return metadataKey;
    }

    public String metadataValue() {
      return metadataValue;
    }
  }

  public static class RemoveFilterParameters extends MuleChainVectorsFilterParameters{

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MuleChainVectorsMetadataKeyProvider.class)
    private String metadataKey;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    private String metadataValue;

    public String metadataKey() {
      return metadataKey;
    }

    public String metadataValue() {
      return metadataValue;
    }
  }
}

