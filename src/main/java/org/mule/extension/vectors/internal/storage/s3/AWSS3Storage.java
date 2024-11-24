package org.mule.extension.vectors.internal.storage.s3;

import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.util.JsonUtils;
import org.mule.extension.vectors.internal.util.MetadatatUtils;
import software.amazon.awssdk.regions.Region;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;

import java.util.Iterator;

import org.json.JSONObject;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AWSS3Storage extends BaseStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSS3Storage.class);

    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;
    private final String awsRegion;

    private String continuationToken = null;

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

    private S3Client s3Client;

    private S3Client getS3Client() {

        if(s3Client == null) {

            // Create S3 client with your credentials
            this.s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)))
                .build();
        }
        return s3Client;
    }

    private Iterator<S3Object> s3ObjectIterator;
    private ListObjectsV2Response response;

    private Iterator<S3Object> getS3ObjectIterator() {


        if(s3ObjectIterator != null && !s3ObjectIterator.hasNext() && continuationToken != null) {
            // Get the continuation token for pagination
            continuationToken = response.nextContinuationToken();
        }

        if(s3ObjectIterator == null || (!s3ObjectIterator.hasNext() && continuationToken != null)) {

            // Build the request
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(getAWSS3Bucket());
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken); // Set continuation token
            }

            ListObjectsV2Request listObjectsV2Request = requestBuilder.build();
            response = getS3Client().listObjectsV2(listObjectsV2Request);

            // Get the list of S3 objects and create an iterator
            this.s3ObjectIterator = response.contents().iterator();
        }
        return s3ObjectIterator;
    }

    public AWSS3Storage(Configuration configuration, String contextPath, String fileType) {

        super(configuration, contextPath, fileType);
        JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
        assert config != null;
        JSONObject storageConfig = config.getJSONObject("S3");
        this.awsAccessKeyId = storageConfig.getString("AWS_ACCESS_KEY_ID");
        this.awsSecretAccessKey = storageConfig.getString("AWS_SECRET_ACCESS_KEY");
        this.awsRegion = storageConfig.getString("AWS_DEFAULT_REGION");
    }

    @Override
    public boolean hasNext() {

        return getS3ObjectIterator().hasNext();
    }

    @Override
    public Document next() {

        S3Object object = getS3ObjectIterator().next();
        LOGGER.debug("AWS S3 Key: " + object.key());
        Document document = getLoader().loadDocument(getAWSS3Bucket(), object.key(), documentParser);
        MetadatatUtils.addMetadataToDocument(document, fileType, object.key());
        return document;
    }

    public Document getSingleDocument() {

        LOGGER.debug("AWS S3 Key: " + getAWSS3ObjectKey());
        Document document = getLoader().loadDocument(getAWSS3Bucket(), getAWSS3ObjectKey(), documentParser);
        MetadatatUtils.addMetadataToDocument(document, fileType, getAWSS3ObjectKey());
        return document;
    }

    private String getAWSS3Bucket() {

        String s3Url = this.contextPath;
        // Remove the "s3://" prefix
        if (s3Url.startsWith("s3://") || s3Url.startsWith("S3://")) {
            s3Url = s3Url.substring(5);
        }
        // Extract the bucket name
        String bucket = s3Url.contains("/") ? s3Url.substring(0, s3Url.indexOf("/")) : s3Url;
        LOGGER.debug("AWS S3 Bucket: " + bucket);
        return bucket;
    }

    private String getAWSS3ObjectKey() {

        String s3Url = this.contextPath;
        // Remove the "s3://" prefix
        if (s3Url.startsWith("s3://") || s3Url.startsWith("S3://")) {
            s3Url = s3Url.substring(5);
        }
        // Extract the bucket name and object key
        int slashIndex = s3Url.indexOf("/");
        String objectKey = slashIndex != -1 ? s3Url.substring(slashIndex + 1) : "";
        LOGGER.debug("AWS S3 Object Key: " + objectKey);
        return objectKey;
    }
}
