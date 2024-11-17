package org.mule.extension.mulechain.vectors.internal.helper.parameter;

import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.provider.FileTypeEmbeddingProvider;
import org.mule.extension.mulechain.vectors.internal.helper.provider.StorageTypeProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class DocumentParameters {

  @Parameter
  @Alias("storageType")
  @DisplayName("Storage Type")
  @Summary("The supported storage types.")
  @Placement(order = 1)
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(StorageTypeProvider.class)
  @Optional(defaultValue = Constants.STORAGE_TYPE_LOCAL)
  private String storageType;

  @Parameter
  @Alias("fileType")
  @DisplayName("File Type")
  @Summary("The supported types of file.")
  @Placement(order = 2)
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(FileTypeEmbeddingProvider.class)
  @Optional(defaultValue = Constants.FILE_TYPE_TEXT)
  private String fileType;

  @Parameter
  @Alias("contextPath")
  @DisplayName("Context Path")
  @Summary("The context path.")
  @Placement(order = 3)
  @Expression(ExpressionSupport.SUPPORTED)
  private String contextPath;

//  @Parameter
//  private String folderPath;

//  @Parameter
//  private String awsS3Bucket;

//  @Parameter
//  private String azureBlobContainer;

  public String getFileType() {
    return fileType;
  }

  public String getStorageType() {
    return storageType;
  }

  public String getContextPath() {
    return contextPath;
  }
}
