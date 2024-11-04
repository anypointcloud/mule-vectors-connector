package org.mule.mulechain.vectors.internal.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import dev.langchain4j.data.document.loader.azure.storage.blob.AzureBlobStorageDocumentLoader;

import java.io.IOException;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.DocumentParser;
import java.util.List;
import org.mule.mulechain.vectors.internal.helpers.FileTypeParameters;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.Document;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class AzureFileReader {

    private final AzureBlobStorageDocumentLoader loader;

    public AzureFileReader(String azureName, String azureKey) {
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
            case "text":
            case "crawl":
                parser = new TextDocumentParser();
                break;
            case "any":
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }

        List<Document> documents = loader.loadDocuments(containerName, parser);
        int fileCount = documents.size();
        System.out.println("Total number of files in '" + containerName + "': " + fileCount);

        long totalFiles = 0;
        for (Document document : documents) {
            totalFiles += 1;
            
            if (fileType.getFileType().equals("crawl")){
                addMetadata(document);
            }
            
            System.out.println("Ingesting File " + totalFiles + ": " + document.metadata().toMap().get("source"));
            ingestor.ingest(document);
        }
        System.out.println("Total number of files processed: " + totalFiles);
        return totalFiles;
    }

    public void readFile(String containerName, String blobName, FileTypeParameters fileType, EmbeddingStoreIngestor ingestor) {
        DocumentParser parser = null;
        switch (fileType.getFileType()){
            case "text":
            case "crawl":
                parser = new TextDocumentParser();
                break;
            case "any":
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }
        Document document = loader.loadDocument(containerName, blobName, parser);
        System.out.println("Ready to add metadata: " + fileType.getFileType());
        
        if (fileType.getFileType().equals("crawl")){
            addMetadata(document);
        }
        
        ingestor.ingest(document);

    }
    
    private void addMetadata(Document document) {
        try {
            String fileContent = document.text();
            JsonNode jsonNode = convertToJson(fileContent.toString());
            String content = jsonNode.path("content").asText();
            String source_url = jsonNode.path("url").asText();
            String title = jsonNode.path("title").asText();
            System.out.println("source: " + source_url);
            System.out.println("title: " + title);
            document.metadata().add("file_type", "text");
            document.metadata().add("file_name", title);
            document.metadata().add("full_path", source_url);
            document.metadata().add("absolute_path", document.ABSOLUTE_DIRECTORY_PATH);
            document.metadata().put("source", source_url);
            document.metadata().add("title", title);
        } catch (IOException e) { 
            System.err.println("Error accessing folder: " + e.getMessage());
        }

    }
  
    private static JsonNode convertToJson(String content) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(content);
  }
}
