package org.mule.extension.vectors.internal.connection.storage.gcs;

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

@Alias("googleCloudStorage")
@DisplayName("Google Cloud Storage")
public class GoogleCloudStorageConnectionProvider extends BaseStorageConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudStorageConnectionProvider.class);

    @ParameterGroup(name = Placement.CONNECTION_TAB)
    private GoogleCloudStorageConnectionParameters googleCloudStorageConnectionParameters;

    @Override
    public BaseStorageConnection connect() throws ConnectionException {
        try {
            GoogleCloudStorageConnection googleCloudStorageConnection = new GoogleCloudStorageConnection(
                    googleCloudStorageConnectionParameters.getProjectId(),
                    googleCloudStorageConnectionParameters.getClientEmail(),
                    googleCloudStorageConnectionParameters.getClientId(),
                    googleCloudStorageConnectionParameters.getPrivateKeyId(),
                    googleCloudStorageConnectionParameters.getPrivateKey()
                    );
            googleCloudStorageConnection.connect();
            return googleCloudStorageConnection;
        } catch (ConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConnectionException("Failed to connect to Google Cloud Storage.", e);
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
                return ConnectionValidationResult.failure("Failed to validate connection to Google Cloud Storage", null);
            }
        } catch (Exception e) {
            return ConnectionValidationResult.failure("Failed to validate connection to Google Cloud Storage", e);
        }
    }
}
