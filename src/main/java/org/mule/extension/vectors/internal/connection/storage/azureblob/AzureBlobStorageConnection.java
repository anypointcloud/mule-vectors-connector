package org.mule.extension.vectors.internal.connection.storage.azureblob;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class AzureBlobStorageConnection implements BaseStorageConnection {

  private String azureName;
  private String azureKey;
  private BlobServiceClient blobServiceClient;

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

  public BlobServiceClient getBlobServiceClient() {
    return blobServiceClient;
  }

  @Override
  public String getStorageType() { return Constants.STORAGE_TYPE_AZURE_BLOB; }

  @Override
  public void connect() {

    this.blobServiceClient = new BlobServiceClientBuilder()
        .endpoint(String.format("https://%s.blob.core.windows.net/", azureName))
        .credential(new StorageSharedKeyCredential(azureName, azureKey))
        .buildClient();
  }

  @Override
  public void disconnect() {

    // Add logic to invalidate connection
  }

  @Override
  public boolean isValid() {

    this.blobServiceClient.listBlobContainers();
    return true;
  }
}
