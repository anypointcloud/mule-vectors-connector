package com.mule.mulechain.vectors.internal.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;
import dev.langchain4j.data.document.DocumentParser;
import java.util.List;

import com.mule.mulechain.vectors.internal.helpers.FileTypeParameters;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.Document;

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
            case "text":
                parser = new TextDocumentParser();
                break;
            case "any":
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
        Document document = loader.loadDocument(bucketName, key, parser);
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
