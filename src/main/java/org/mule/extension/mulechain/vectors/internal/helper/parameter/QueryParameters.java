package org.mule.extension.mulechain.vectors.internal.helper.parameter;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class QueryParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Summary("The embedding page size used when querying the vector store. Defaults to 5000 embeddings.")
  @Optional(defaultValue = "5000")
  private Number embeddingPageSize;

//  @Parameter
//  @Expression(ExpressionSupport.SUPPORTED)
//  @Summary("The offset used when querying the vector store")
//  @Optional(defaultValue = "0")
//  private Number offset;

//  @Parameter
//  @Expression(ExpressionSupport.SUPPORTED)
//  @Summary("The limit applied used when querying the vector store")
//  @Optional
//  private Number limit;

  public int embeddingPageSize() {return embeddingPageSize != null ? embeddingPageSize.intValue() : 5000;}

//  public int offset() {
//    return offset.intValue();
//  }

//  public int limit() {
//    return limit.intValue();
//  }
}
