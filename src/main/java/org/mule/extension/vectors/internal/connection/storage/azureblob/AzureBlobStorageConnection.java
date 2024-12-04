package org.mule.extension.vectors.internal.connection.storage.azureblob;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

@Alias("azureBlob")
@DisplayName("Azure Blob")
public class AzureBlobStorageConnection implements BaseStorageConnection {

  private String azureName;
  private String azureKey;

  public AzureBlobStorageConnection(String azureName, String azureKey) {
    this.azureName = azureName;
    this.azureKey = azureKey;
  }

  public String getAzureName() {
    return azureName;
  }

  public String getAzureKey() {
    return azureKey;
  }

  @Override
  public String getStorageType() { return Constants.STORAGE_TYPE_AZURE_BLOB; }
}
