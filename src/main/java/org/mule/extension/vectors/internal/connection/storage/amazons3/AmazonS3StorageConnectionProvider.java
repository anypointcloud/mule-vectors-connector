package org.mule.extension.vectors.internal.connection.storage.amazons3;

import org.mule.extension.vectors.internal.connection.model.einstein.EinsteinModelConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.utils.StringUtils;


import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Alias("amazonS3")
@DisplayName("Amazon S3")
public class AmazonS3StorageConnectionProvider extends BaseStorageConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3StorageConnectionProvider.class);

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private AmazonS3StorageConnectionParameters amazonS3StorageConnectionParameters;

  @Override
  public BaseStorageConnection connect() throws ConnectionException {

    try {

      S3Client s3Client = S3Client.builder()
          .region(Region.of(amazonS3StorageConnectionParameters.getAwsRegion()))
          .credentialsProvider(StaticCredentialsProvider.create(
              AwsBasicCredentials.create(amazonS3StorageConnectionParameters.getAwsAccessKeyId(),
                                         amazonS3StorageConnectionParameters.getAwsSecretAccessKey())))
          .build();

      return new AmazonS3StorageConnection(
          amazonS3StorageConnectionParameters.getAwsRegion(),
          amazonS3StorageConnectionParameters.getAwsAccessKeyId(),
          amazonS3StorageConnectionParameters.getAwsSecretAccessKey(),
          s3Client
      );

    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Amazon S3. Test Connection not supported yet.", e);
    }
  }

  @Override
  public void disconnect(BaseStorageConnection connection) {

    S3Client s3Client = ((AmazonS3StorageConnection)connection).getS3Client();
    if (s3Client != null) {

      s3Client.close();
    }
  }

  @Override
  public ConnectionValidationResult validate(BaseStorageConnection connection) {

    try {

      S3Client s3Client = ((AmazonS3StorageConnection)connection).getS3Client();
      s3Client.listBuckets(ListBucketsRequest.builder().build());
      return ConnectionValidationResult.success();

    } catch (SdkException e) {

      return ConnectionValidationResult.failure("Failed to validate connection to Amazon S3.", e);
    }
  }
}
