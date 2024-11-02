package com.mule.mulechain.vectors.internal;

import com.mule.mulechain.vectors.internal.providers.MuleChainVectorsParameterEModelNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class MuleChainVectorsModelParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(MuleChainVectorsParameterEModelNameProvider.class)
  private String modelName;

  public String getModelName() {
    return modelName;
  }

}
