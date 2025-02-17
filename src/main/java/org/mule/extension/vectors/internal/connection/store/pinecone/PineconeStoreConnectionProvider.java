package org.mule.extension.vectors.internal.connection.store.pinecone;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.connection.store.BaseStoreConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Alias("pinecone")
@DisplayName("Pinecone")
public class PineconeStoreConnectionProvider  extends BaseStoreConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(PineconeStoreConnectionProvider.class);

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private PineconeStoreConnectionParameters pineconeStoreConnectionParameters;

  @Override
  public BaseStoreConnection connect() throws ConnectionException {

    try {

      PineconeStoreConnection pineconeStoreConnection =
          new PineconeStoreConnection(pineconeStoreConnectionParameters.getCloud(),
                                      pineconeStoreConnectionParameters.getRegion(),
                                      pineconeStoreConnectionParameters.getApiKey());
      pineconeStoreConnection.connect();
      return pineconeStoreConnection;

    } catch (ConnectionException e) {

      throw e;

    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Pinecone", e);
    }
  }

  @Override
  public void disconnect(BaseStoreConnection connection) {

    try {

      connection.disconnect();
    } catch (Exception e) {

      LOGGER.error("Failed to close connection", e);
    }
  }

  @Override
  public ConnectionValidationResult validate(BaseStoreConnection connection) {

    try {

      if (connection.isValid()) {
        return ConnectionValidationResult.success();
      } else {
        return ConnectionValidationResult.failure("Failed to validate connection to Pinecone", null);
      }
    } catch (Exception e) {
      return ConnectionValidationResult.failure("Failed to validate connection to Pinecone", e);
    }
  }

}
