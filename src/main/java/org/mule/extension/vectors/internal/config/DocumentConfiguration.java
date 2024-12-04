package org.mule.extension.vectors.internal.config;

import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnectionProvider;
import org.mule.extension.vectors.internal.connection.storage.azureblob.AzureBlobStorageConnectionProvider;
import org.mule.extension.vectors.internal.operation.DocumentOperations;
import org.mule.extension.vectors.internal.operation.EmbeddingOperations;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@org.mule.runtime.extension.api.annotation.Configuration(name = "documentConfig")
@ConnectionProviders({
    AmazonS3StorageConnectionProvider.class,
    AzureBlobStorageConnectionProvider.class})
@Operations({DocumentOperations.class})
public class DocumentConfiguration {

}
