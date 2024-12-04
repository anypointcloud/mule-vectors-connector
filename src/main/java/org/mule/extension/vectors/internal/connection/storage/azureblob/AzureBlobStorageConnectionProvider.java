package org.mule.extension.vectors.internal.connection.storage.azureblob;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnectionProvider;
import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnection;
import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnectionParameters;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Alias("azureBlob")
@DisplayName("Azure Blob")
public class AzureBlobStorageConnectionProvider extends BaseStorageConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorageConnectionProvider.class);

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private AzureBlobStorageConnectionParameters azureBlobStorageConnectionParameters;

  @Override
  public BaseStorageConnection connect() throws ConnectionException {

    try {

      AzureBlobStorageConnection azureBlobStorageConnection = new AzureBlobStorageConnection(azureBlobStorageConnectionParameters.getAzureName(),
                                                                                             azureBlobStorageConnectionParameters.getAzureKey());
      azureBlobStorageConnection.connect();
      return azureBlobStorageConnection;

    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Azure Blob.", e);
    }
  }

  @Override
  public void disconnect(BaseStorageConnection connection) {

    connection.disconnect();
  }

  @Override
  public ConnectionValidationResult validate(BaseStorageConnection connection) {

    try {

      if (connection.isValid()) {
        return ConnectionValidationResult.success();
      } else {
        return ConnectionValidationResult.failure("Failed to validate connection to Azure Blob", null);
      }
    } catch (Exception e) {
      return ConnectionValidationResult.failure("Failed to validate connection to Azure Blob", e);
    }
  }
}
