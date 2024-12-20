package org.mule.extension.vectors.internal.storage.amazons3;

import dev.langchain4j.data.document.BlankDocumentException;
import org.mule.extension.vectors.internal.config.DocumentConfiguration;
import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnection;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.util.MetadataUtils;
import org.mule.runtime.extension.api.exception.ModuleException;
import software.amazon.awssdk.regions.Region;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;

import java.util.Iterator;

import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AmazonS3Storage extends BaseStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Storage.class);

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

            loader = new AmazonS3DocumentLoader(s3Client);
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

    public AmazonS3Storage(DocumentConfiguration documentConfiguration, AmazonS3StorageConnection amazonS3StorageConnection, String contextPath, String fileType) {

        super(documentConfiguration, amazonS3StorageConnection, contextPath, fileType);
        this.awsAccessKeyId = amazonS3StorageConnection.getAwsAccessKeyId();
        this.awsSecretAccessKey = amazonS3StorageConnection.getAwsSecretAccessKey();
        this.awsRegion = amazonS3StorageConnection.getAwsRegion();
        this.s3Client = amazonS3StorageConnection.getS3Client();
    }

    @Override
    public boolean hasNext() {

        return getS3ObjectIterator().hasNext();
    }

    @Override
    public Document next() {

        S3Object object = getS3ObjectIterator().next();
        LOGGER.debug("AWS S3 Object Key: " + object.key());
        Document document;
        try {
            document = getLoader().loadDocument(getAWSS3Bucket(), object.key(), documentParser);
        } catch(BlankDocumentException bde) {

            LOGGER.warn(String.format("BlankDocumentException: Error while parsing document %s.", contextPath));
            throw bde;
        } catch (Exception e) {

            throw new ModuleException(
                String.format("Error while parsing document %s.", contextPath),
                MuleVectorsErrorType.DOCUMENT_PARSING_FAILURE,
                e);
        }
        MetadataUtils.addMetadataToDocument(document, fileType, object.key());
        return document;
    }

    public Document getSingleDocument() {

        LOGGER.debug("S3 URL: " + contextPath);
        Document document = getLoader().loadDocument(getAWSS3Bucket(), getAWSS3ObjectKey(), documentParser);
        MetadataUtils.addMetadataToDocument(document, fileType, getAWSS3ObjectKey());
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
        return objectKey;
    }
}
