package org.mule.extension.vectors.internal.connection.storage.amazons3;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;

public class AmazonS3StorageConnection implements BaseStorageConnection {

  private String awsRegion;
  private String awsAccessKeyId;
  private String awsSecretAccessKey;

  public AmazonS3StorageConnection(String awsRegion, String awsAccessKeyId, String awsSecretAccessKey) {
    this.awsRegion = awsRegion;
    this.awsAccessKeyId = awsAccessKeyId;
    this.awsSecretAccessKey = awsSecretAccessKey;
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

  @Override
  public String getStorageType() {
    return Constants.STORAGE_TYPE_AWS_S3;
  }
}
