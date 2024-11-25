package org.mule.extension.mulechain.vectors.internal.helper.parameter;

import org.mule.extension.mulechain.vectors.internal.helper.provider.EmbeddingModelNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class EmbeddingModelParameters {

  @Parameter
  @Alias("embeddingModelName")
  @DisplayName("Embedding Model Name")
  @Summary("The embedding model name.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(EmbeddingModelNameProvider.class)
  private String embeddingModelName;

  public String getEmbeddingModelName() {
    return embeddingModelName;
  }

}
