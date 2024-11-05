package org.mule.extension.mulechain.vectors.internal.storage;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;
import dev.langchain4j.data.document.DocumentParser;
import java.util.List;

import org.mule.extension.mulechain.vectors.internal.constants.MuleChainVectorsConstants;
import org.mule.extension.mulechain.vectors.internal.helpers.parameters.FileTypeParameters;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class S3FileReader {

    private final String bucketName;
    private final AmazonS3DocumentLoader loader;

    public S3FileReader(String bucketName, String awsKey, String awsSecret, String awsRegion) {
        this.bucketName = bucketName;
        AwsCredentials creds = new AwsCredentials(awsKey, awsSecret);
        this.loader = AmazonS3DocumentLoader.builder()
                    .region(awsRegion)
                    .awsCredentials(creds)
                    .build();
    }

    public long readAllFiles(String folderPath, EmbeddingStoreIngestor ingestor, FileTypeParameters fileType)
    {
        DocumentParser parser = null;
        switch (fileType.getFileType()){
            case MuleChainVectorsConstants.FILE_TYPE_TEXT:
                parser = new TextDocumentParser();
                break;
            case MuleChainVectorsConstants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }

        List<Document> documents = loader.loadDocuments(bucketName, folderPath, parser);
        int fileCount = documents.size();
        System.out.println("Total number of files in '" + folderPath + "': " + fileCount);

        long totalFiles = 0;
        for (Document document : documents) {
            ingestor.ingest(document);
            totalFiles += 1;
            System.out.println("Ingesting File " + totalFiles + ": " + document.metadata().toMap().get("source"));
        }
        System.out.println("Total number of files processed: " + totalFiles);
        return totalFiles;
    }

    public void readFile(String key, FileTypeParameters fileType, EmbeddingStoreIngestor ingestor) {
        DocumentParser parser = null;
        switch (fileType.getFileType()){
            case MuleChainVectorsConstants.FILE_TYPE_TEXT:
            case MuleChainVectorsConstants.FILE_TYPE_CRAWL:
                parser = new TextDocumentParser();
                break;
            case MuleChainVectorsConstants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }
        Document document = loader.loadDocument(bucketName, key, parser);
        if (fileType.getFileType().equals(MuleChainVectorsConstants.FILE_TYPE_CRAWL)){
            addMetadata(document);
        }
        ingestor.ingest(document);
    }
    private void addMetadata(Document document) {
        try {
            String fileContent = document.text();
            JsonNode jsonNode = JsonUtils.stringToJsonNode(fileContent.toString());
            String content = jsonNode.path("content").asText();
            String source_url = jsonNode.path("url").asText();
            String title = jsonNode.path("title").asText();
            document.metadata().add(MuleChainVectorsConstants.METADATA_KEY_FILE_TYPE, MuleChainVectorsConstants.FILE_TYPE_TEXT);
            document.metadata().add(MuleChainVectorsConstants.METADATA_KEY_FILE_NAME, title);
            document.metadata().add(MuleChainVectorsConstants.METADATA_KEY_FULL_PATH, source_url);
            document.metadata().put("source", source_url);
            document.metadata().add("title", title);
        } catch (IOException e) { 
            System.err.println("Error accessing folder: " + e.getMessage());
        }

    }
}
