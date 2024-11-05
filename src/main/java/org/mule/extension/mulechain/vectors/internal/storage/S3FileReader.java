package org.mule.extension.mulechain.vectors.internal.storage;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;
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

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class S3FileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileReader.class);

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
            case Constants.FILE_TYPE_TEXT:
                parser = new TextDocumentParser();
                break;
            case Constants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }

        List<Document> documents = loader.loadDocuments(bucketName, folderPath, parser);
        int fileCount = documents.size();
        LOGGER.debug("Total number of files in '" + folderPath + "': " + fileCount);

        long totalFiles = 0;
        for (Document document : documents) {
            ingestor.ingest(document);
            totalFiles += 1;
            LOGGER.debug("Ingesting File " + totalFiles + ": " + document.metadata().toMap().get("source"));
        }
        LOGGER.debug("Total number of files processed: " + totalFiles);
        return totalFiles;
    }

    public void readFile(String key, FileTypeParameters fileType, EmbeddingStoreIngestor ingestor) {
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
        Document document = loader.loadDocument(bucketName, key, parser);
        if (fileType.getFileType().equals(Constants.FILE_TYPE_CRAWL)){
            DocumentUtils.addMetadataToDocument(document);
        }
        ingestor.ingest(document);
    }
}
