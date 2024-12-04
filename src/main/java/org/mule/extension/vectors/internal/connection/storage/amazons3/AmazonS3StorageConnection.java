package org.mule.extension.vectors.internal.connection.storage.amazons3;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

@Alias("amazonS3")
@DisplayName("Amazon S3")
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
