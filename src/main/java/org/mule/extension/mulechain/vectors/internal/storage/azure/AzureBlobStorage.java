package org.mule.extension.mulechain.vectors.internal.storage.azure;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import dev.langchain4j.data.document.loader.azure.storage.blob.AzureBlobStorageDocumentLoader;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.DocumentParser;
import java.util.List;

import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.FileTypeParameters;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.util.DocumentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureBlobStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorage.class);

    private final AzureBlobStorageDocumentLoader loader;

    public AzureBlobStorage(String azureName, String azureKey) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(azureName, azureKey);

        // Azure SDK client builders accept the credential as a parameter
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", azureName))
                .credential(credential)
                .buildClient();

        this.loader = new AzureBlobStorageDocumentLoader(blobServiceClient);
    }

    public static BlobServiceClient GetBlobServiceClientAccountKey(String accountName, String accountKey) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

        // Azure SDK client builders accept the credential as a parameter
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", accountName))
                .credential(credential)
                .buildClient();

        return blobServiceClient;        
    }

    public long readAllFiles(String containerName, EmbeddingStoreIngestor ingestor, FileTypeParameters fileType)
    {
        DocumentParser parser = null;
        switch (fileType.getFileType()){
            case Constants.FILE_TYPE_TEXT:
            case Constants.FILE_TYPE_CRAWL:
                parser = new TextDocumentParser();
                break;
            case Constants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }

        List<Document> documents = loader.loadDocuments(containerName, parser);
        int fileCount = documents.size();
        LOGGER.debug("Total number of files in '" + containerName + "': " + fileCount);

        long totalFiles = 0;
        for (Document document : documents) {
            totalFiles += 1;
            
            if (fileType.getFileType().equals(Constants.FILE_TYPE_CRAWL)){
                DocumentUtils.addMetadataToDocument(document);
            }

            LOGGER.debug("Ingesting File " + totalFiles + ": " + document.metadata().toMap().get("source"));
            ingestor.ingest(document);
        }
        LOGGER.debug("Total number of files processed: " + totalFiles);
        return totalFiles;
    }

    public void readFile(String containerName, String blobName, FileTypeParameters fileType, EmbeddingStoreIngestor ingestor) {
        DocumentParser parser = null;
        switch (fileType.getFileType()){
            case Constants.FILE_TYPE_TEXT:
            case Constants.FILE_TYPE_CRAWL:
                parser = new TextDocumentParser();
                break;
            case Constants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }
        Document document = loader.loadDocument(containerName, blobName, parser);
        System.out.println("Ready to add metadata: " + fileType.getFileType());
        
        if (fileType.getFileType().equals(Constants.FILE_TYPE_CRAWL)){
            DocumentUtils.addMetadataToDocument(document);
        }
        
        ingestor.ingest(document);

    }
}
