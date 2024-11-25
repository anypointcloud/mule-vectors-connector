package org.mule.extension.mulechain.vectors.internal.helper.parameter;

import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.provider.FileTypeEmbeddingProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class FileTypeParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(FileTypeEmbeddingProvider.class)
  @Optional(defaultValue = Constants.FILE_TYPE_TEXT)
  private String fileType;

  public String getFileType() {
    return fileType;
  }

}
