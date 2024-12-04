package org.mule.extension.vectors.internal.connection.storage.amazons3;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import software.amazon.awssdk.services.s3.S3Client;

public class AmazonS3StorageConnection implements BaseStorageConnection {

  private String awsRegion;
  private String awsAccessKeyId;
  private String awsSecretAccessKey;
  private S3Client s3Client;

  public AmazonS3StorageConnection(String awsRegion, String awsAccessKeyId, String awsSecretAccessKey, S3Client s3Client) {
    this.awsRegion = awsRegion;
    this.awsAccessKeyId = awsAccessKeyId;
    this.awsSecretAccessKey = awsSecretAccessKey;
    this.s3Client = s3Client;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public String getAwsAccessKeyId() {
    return awsAccessKeyId;
  }

  public String getAwsSecretAccessKey() {
    return awsSecretAccessKey;
  }

  public S3Client getS3Client() {
    return s3Client;
  }

  @Override
  public String getStorageType() {
    return Constants.STORAGE_TYPE_AWS_S3;
  }
}
