package org.mule.extension.mulechain.vectors.internal.storage.azureblob;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.StorageSharedKeyCredential;
import dev.langchain4j.data.document.loader.azure.storage.blob.AzureBlobStorageDocumentLoader;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.DocumentParser;
import java.util.List;

import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.storage.BaseStorage;
import org.mule.extension.mulechain.vectors.internal.util.DocumentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class AzureBlobStorage extends BaseStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorage.class);

    private final String azureName;
    private final String azureKey;

    private StorageSharedKeyCredential getCredentials() {
        return new StorageSharedKeyCredential(azureName, azureKey);
    }

    private BlobServiceClient blobServiceClient;

    private BlobServiceClient getBlobServiceClient() {

        if(this.blobServiceClient == null) {

            // Azure SDK client builders accept the credential as a parameter
            this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", azureName))
                .credential(getCredentials())
                .buildClient();
        }
        return this.blobServiceClient;
    }

    private AzureBlobStorageDocumentLoader loader;

    private AzureBlobStorageDocumentLoader getLoader() {

        if(this.loader == null) {

            this.loader = new AzureBlobStorageDocumentLoader(getBlobServiceClient());
        }
        return this.loader;
    }

    public AzureBlobStorage(Configuration configuration, String storeName, EmbeddingStoreIngestor embeddingStoreIngestor) {

        super(configuration, storeName, embeddingStoreIngestor);
        JSONObject config = readConfigFile(configuration.getConfigFilePath());
        assert config != null;
        JSONObject storageConfig = config.getJSONObject("AZURE_BLOB");
        this.azureName = storageConfig.getString("AZURE_BLOB_ACCOUNT_NAME");
        this.azureKey = storageConfig.getString("AZURE_BLOB_ACCOUNT_KEY");
    }

    public JSONObject readAndIngestAllFiles(String containerName, String fileType) {

        DocumentParser documentParser = getDocumentParser(fileType);

        // Get a BlobContainerClient
        BlobContainerClient containerClient = getBlobServiceClient().getBlobContainerClient(containerName);

        long totalFiles = 0;

        // List all blobs in the container
        for (BlobItem blobItem : containerClient.listBlobs()) {

            LOGGER.debug("Blob name: " + blobItem.getName());
            Document document = getLoader().loadDocument(containerName, blobItem.getName(), documentParser);
            DocumentUtils.addMetadataToDocument(document, fileType, blobItem.getName());
            embeddingStoreIngestor.ingest(document);
            totalFiles += 1;
        }

        LOGGER.debug("Total number of files processed: " + totalFiles);
        return createFolderIngestionStatusObject(totalFiles, fileType);
    }

    public JSONObject readAndIngestFile(String contextPath, String fileType) {

        String[] parts = contextPath.split("/", 2);
        String containerName = parts[0];
        String blobName = parts[1];

        DocumentParser documentParser = getDocumentParser(fileType);

        Document document = getLoader().loadDocument(containerName, blobName, documentParser);
        DocumentUtils.addMetadataToDocument(document, fileType, blobName);
        embeddingStoreIngestor.ingest(document);
        return createFileIngestionStatusObject(fileType, containerName);
    }
}
