package org.mule.extension.mulechain.vectors.internal.storage.s3;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;
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

public class AWSS3Storage extends BaseStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSS3Storage.class);

    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;
    private final String awsRegion;
    private final String awsS3Bucket;

    private AwsCredentials getCredentials() {
        return new AwsCredentials(awsAccessKeyId, awsSecretAccessKey);
    }

    private AmazonS3DocumentLoader loader;

    private AmazonS3DocumentLoader getLoader() {

        if(loader == null) {

            loader = AmazonS3DocumentLoader.builder()
                .region(awsRegion)
                .awsCredentials(getCredentials())
                .build();
        }
        return loader;
    }

    public AWSS3Storage(Configuration configuration, String storeName, EmbeddingStoreIngestor embeddingStoreIngestor) {

        super(configuration, storeName, embeddingStoreIngestor);
        JSONObject config = readConfigFile(configuration.getConfigFilePath());
        assert config != null;
        JSONObject storageConfig = config.getJSONObject("S3");
        this.awsAccessKeyId = storageConfig.getString("AWS_ACCESS_KEY_ID");
        this.awsSecretAccessKey = storageConfig.getString("AWS_SECRET_ACCESS_KEY");
        this.awsRegion = storageConfig.getString("AWS_DEFAULT_REGION");
        this.awsS3Bucket = storageConfig.getString("AWS_S3_BUCKET");
    }

    public JSONObject readAndIngestAllFiles(String folderPath, String fileType) {

        DocumentParser parser = null;
        switch (fileType){
            case Constants.FILE_TYPE_TEXT:
                parser = new TextDocumentParser();
                break;
            case Constants.FILE_TYPE_ANY:
                parser = new ApacheTikaDocumentParser();
                break;
            default:
                throw new IllegalArgumentException("Unsupported File Type: " + fileType);
        }

        List<Document> documents = getLoader().loadDocuments(awsS3Bucket, folderPath, parser);
        int fileCount = documents.size();
        LOGGER.debug("Total number of files in '" + folderPath + "': " + fileCount);

        long totalFiles = 0;
        for (Document document : documents) {
            embeddingStoreIngestor.ingest(document);
            totalFiles += 1;
            LOGGER.debug("Ingesting File " + totalFiles + ": " + document.metadata().toMap().get("source"));
        }
        LOGGER.debug("Total number of files processed: " + totalFiles);
        return createFolderIngestionStatusObject(totalFiles, fileType);
    }

    public JSONObject readAndIngestFile(String key, String fileType) {
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
        Document document = getLoader().loadDocument(awsS3Bucket, key, parser);
        if (fileType.equals(Constants.FILE_TYPE_CRAWL)){
            DocumentUtils.addMetadataToDocument(document);
        }
        embeddingStoreIngestor.ingest(document);
        return createFileIngestionStatusObject(fileType, key);
    }
}
