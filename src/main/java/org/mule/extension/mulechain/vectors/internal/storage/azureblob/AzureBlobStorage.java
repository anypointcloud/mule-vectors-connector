package org.mule.extension.mulechain.vectors.internal.storage.azureblob;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
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

    private AzureBlobStorageDocumentLoader loader;

    private AzureBlobStorageDocumentLoader getLoader() {

        if(this.loader == null) {

            // Azure SDK client builders accept the credential as a parameter
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", azureName))
                .credential(getCredentials())
                .buildClient();

            this.loader = new AzureBlobStorageDocumentLoader(blobServiceClient);
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
        DocumentParser parser = null;
        switch (fileType){
            case Constants.FILE_TYPE_TEXT:
            case Constants.FILE_TYPE_CRAWL:
                parser = new TextDocumentParser();
                break;
            case Constants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType);
        }

        List<Document> documents = getLoader().loadDocuments(containerName, parser);
        int fileCount = documents.size();
        LOGGER.debug("Total number of files in '" + containerName + "': " + fileCount);

        long totalFiles = 0;
        for (Document document : documents) {
            totalFiles += 1;
            
            if (fileType.equals(Constants.FILE_TYPE_CRAWL)){
                DocumentUtils.addMetadataToDocument(document);
            }

            LOGGER.debug("Ingesting File " + totalFiles + ": " + document.metadata().toMap().get("source"));
            embeddingStoreIngestor.ingest(document);
        }
        LOGGER.debug("Total number of files processed: " + totalFiles);
        return createFolderIngestionStatusObject(totalFiles, fileType);
    }

    public JSONObject readAndIngestFile(String contextPath, String fileType) {

        String[] parts = contextPath.split("/", 2);
        String containerName = parts[0];
        String blobName = parts[1];

        DocumentParser parser = null;
        switch (fileType){
            case Constants.FILE_TYPE_TEXT:
            case Constants.FILE_TYPE_CRAWL:
                parser = new TextDocumentParser();
                break;
            case Constants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType);
        }
        Document document = getLoader().loadDocument(containerName, blobName, parser);
        System.out.println("Ready to add metadata: " + fileType);
        
        if (fileType.equals(Constants.FILE_TYPE_CRAWL)){
            DocumentUtils.addMetadataToDocument(document);
        }
        
        embeddingStoreIngestor.ingest(document);
        return createFileIngestionStatusObject(fileType, containerName);
    }
}
