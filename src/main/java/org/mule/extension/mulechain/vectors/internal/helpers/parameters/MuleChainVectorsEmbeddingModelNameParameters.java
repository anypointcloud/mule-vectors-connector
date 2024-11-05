package org.mule.extension.mulechain.vectors.internal.helpers.parameters;

import org.mule.extension.mulechain.vectors.internal.helpers.providers.MuleChainVectorsEmbeddingModelNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class MuleChainVectorsEmbeddingModelNameParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(MuleChainVectorsEmbeddingModelNameProvider.class)
  private String embeddingModelName;

  public String getEmbeddingModelName() {
    return embeddingModelName;
  }

}
