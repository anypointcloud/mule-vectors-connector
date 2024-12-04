package org.mule.extension.vectors.internal.connection.storage.azureblob;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Alias("azureBlob")
@DisplayName("Azure Blob")
public class AzureBlobStorageConnectionProvider extends BaseStorageConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorageConnectionProvider.class);

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private AzureBlobStorageConnectionParameters azureBlobStorageConnectionParameters;

  @Override
  public BaseStorageConnection connect() throws ConnectionException {

    throw new ConnectionException("Failed to connect to Azure Blob. Test Connection not supported yet.", null);
  }

  @Override
  public void disconnect(BaseStorageConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(BaseStorageConnection connection) {

    return ConnectionValidationResult.failure("Failed to validate connection to Azure Blob.", null);
  }
}
