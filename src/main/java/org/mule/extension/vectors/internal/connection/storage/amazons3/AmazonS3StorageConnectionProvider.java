package org.mule.extension.vectors.internal.connection.storage.amazons3;

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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Alias("amazonS3")
@DisplayName("Amazon S3")
public class AmazonS3StorageConnectionProvider extends BaseStorageConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3StorageConnectionProvider.class);

  private static final String SERVICE = "s3";

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private AmazonS3StorageConnectionParameters amazonS3StorageConnectionParameters;

  @Override
  public BaseStorageConnection connect() throws ConnectionException {

    try {

      int responseCode = getConnectionResponseCode(amazonS3StorageConnectionParameters.getAwsRegion(),
                                                   amazonS3StorageConnectionParameters.getAwsAccessKeyId(),
                                                   amazonS3StorageConnectionParameters.getAwsSecretAccessKey());
      if (responseCode == 200) {
        return new AmazonS3StorageConnection(
            amazonS3StorageConnectionParameters.getAwsRegion(),
            amazonS3StorageConnectionParameters.getAwsAccessKeyId(),
            amazonS3StorageConnectionParameters.getAwsSecretAccessKey());
      } else {
        throw new ConnectionException("Failed to connect to Amazon S3: HTTP " + responseCode);
      }
    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Amazon S3", e);
    }
  }

  @Override
  public void disconnect(BaseStorageConnection connection) {

    try {
      // Add logic to invalidate the connection if necessary
    } catch (Exception e) {

      LOGGER.error("Error while disconnecting [{}]: {}", amazonS3StorageConnectionParameters.getAwsAccessKeyId(), e.getMessage(), e);
    }
  }

  @Override
  public ConnectionValidationResult validate(BaseStorageConnection connection) {

    try {
      int responseCode =
          getConnectionResponseCode(
              amazonS3StorageConnectionParameters.getAwsRegion(),
              amazonS3StorageConnectionParameters.getAwsAccessKeyId(),
              amazonS3StorageConnectionParameters.getAwsSecretAccessKey());

      if (responseCode == 200) {
        return ConnectionValidationResult.success();
      } else {
        return ConnectionValidationResult.failure("Failed to validate connection to Amazon S3: HTTP " + responseCode, null);
      }
    } catch (Exception e) {
      return ConnectionValidationResult.failure("Failed to validate connection to Amazon S3", e);
    }
  }

  private int getConnectionResponseCode(String awsRegion, String awsAccessKeyId, String awsSecretAccessKey) throws Exception {

    Instant now = Instant.now();
    String amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC)
        .format(now);

    String dateStamp = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneOffset.UTC)
        .format(now);

    String method = "GET";
    String canonicalUri = "/";
    String canonicalQueryString = "list-type=2";
    String host = "s3." + awsRegion + ".amazonaws.com";

    String payloadHash = hashPayload("");

    String canonicalHeaders = "host:" + host + "\n" +
        "x-amz-content-sha256:" + payloadHash + "\n" +
        "x-amz-date:" + amzDate + "\n";
    String signedHeaders = "host;x-amz-content-sha256;x-amz-date";

    String canonicalRequest = method + "\n" +
        canonicalUri + "\n" +
        canonicalQueryString + "\n" +
        canonicalHeaders + "\n" +
        signedHeaders + "\n" +
        payloadHash;

    String algorithm = "AWS4-HMAC-SHA256";
    String credentialScope = dateStamp + "/" + awsRegion + "/" + SERVICE + "/aws4_request";
    String stringToSign = algorithm + "\n" +
        amzDate + "\n" +
        credentialScope + "\n" +
        hashCanonicalRequest(canonicalRequest);

    byte[] signingKey = getSignatureKey(awsSecretAccessKey, dateStamp, awsRegion, SERVICE);
    String signature = bytesToHex(hmacSHA256(signingKey, stringToSign));

    String authHeader = algorithm +
        " Credential=" + awsAccessKeyId + "/" + credentialScope +
        ", SignedHeaders=" + signedHeaders +
        ", Signature=" + signature;

    String fullUrl = "https://" + SERVICE + "." + awsRegion + ".amazonaws.com/?list-type=2";
    URL url = new URL(fullUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", authHeader);
    connection.setRequestProperty("x-amz-date", amzDate);
    connection.setRequestProperty("x-amz-content-sha256", payloadHash);
    connection.setRequestProperty("Host", host);

    int responseCode = connection.getResponseCode();
    LOGGER.debug("Response Code: " + responseCode);

    if(responseCode != HttpURLConnection.HTTP_OK) {

      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));

      String inputLine;
      StringBuilder response = new StringBuilder();
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();

      LOGGER.debug("Error Response: " + response);
    }

    return responseCode;

  }

  // Helper methods for AWS Signature Version 4 signing process
  private String getDate(String timestamp) {
    return timestamp.substring(0, 10).replace("-", "");
  }

  private byte[] hmacSHA256(byte[] key, String data) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return mac.doFinal(data.getBytes("UTF-8"));
  }

  private byte[] getSignatureKey(String awsSecretAccessKey, String date, String region, String service) throws Exception {
    byte[] kSecret = ("AWS4" + awsSecretAccessKey).getBytes("UTF-8");
    byte[] kDate = hmacSHA256(kSecret, date);
    byte[] kRegion = hmacSHA256(kDate, region);
    byte[] kService = hmacSHA256(kRegion, service);
    return hmacSHA256(kService, "aws4_request");
  }

  private String hashPayload(String payload) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(payload.getBytes("UTF-8"));
    return bytesToHex(hash);
  }

  private String hashCanonicalRequest(String canonicalRequest) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(canonicalRequest.getBytes("UTF-8"));
    return bytesToHex(hash);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
