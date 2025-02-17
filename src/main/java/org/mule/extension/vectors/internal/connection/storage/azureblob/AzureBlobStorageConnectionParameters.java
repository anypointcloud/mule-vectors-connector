package org.mule.extension.vectors.internal.connection.storage.azureblob;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnectionParameters;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public class AzureBlobStorageConnectionParameters extends BaseStorageConnectionParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 1)
  @Example("<your-account-name>")
  private String azureName;

  @Parameter
  @Password
  @Placement(order = 2)
  @Example("<your-account-key>")
  private String azureKey;

  public String getAzureName() {
    return azureName;
  }

  public String getAzureKey() {
    return azureKey;
  }
}
