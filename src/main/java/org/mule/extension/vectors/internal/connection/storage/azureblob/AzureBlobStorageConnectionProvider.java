package org.mule.extension.vectors.internal.connection.storage.azureblob;

import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnectionProvider;
import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnection;
import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnectionParameters;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Alias("azureBlob")
@DisplayName("Azure Blob")
public class AzureBlobStorageConnectionProvider extends BaseStorageConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorageConnectionProvider.class);

  private static final String API_VERSION = "2021-04-10";
  private static final String AUTHORIZATION_SCHEME = "SharedKey";
  private static final String SIGNATURE_METHOD = "HmacSHA256";

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private AzureBlobStorageConnectionParameters azureBlobStorageConnectionParameters;

  @Override
  public BaseStorageConnection connect() throws ConnectionException {

    try {

      int responseCode = getConnectionResponseCode(azureBlobStorageConnectionParameters.getAzureName(),
                                                   azureBlobStorageConnectionParameters.getAzureKey());
      if (responseCode == 200) {
        return new AzureBlobStorageConnection(azureBlobStorageConnectionParameters.getAzureName(),
                                              azureBlobStorageConnectionParameters.getAzureKey());
      } else {
        throw new ConnectionException("Failed to connect to Azure Blob: HTTP " + responseCode);
      }
    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Azure Blob", e);
    }
  }

  @Override
  public void disconnect(BaseStorageConnection connection) {

    try {
      // Add logic to invalidate the connection if necessary
    } catch (Exception e) {

      LOGGER.error("Error while disconnecting [{}]: {}", azureBlobStorageConnectionParameters.getAzureName(), e.getMessage(), e);
    }
  }

  @Override
  public ConnectionValidationResult validate(BaseStorageConnection connection) {

    try {
      int responseCode =
          getConnectionResponseCode(azureBlobStorageConnectionParameters.getAzureName(),
                                    azureBlobStorageConnectionParameters.getAzureKey());

      if (responseCode == 200) {
        return ConnectionValidationResult.success();
      } else {
        return ConnectionValidationResult.failure("Failed to validate connection to Azure Blob: HTTP " + responseCode, null);
      }
    } catch (Exception e) {
      return ConnectionValidationResult.failure("Failed to validate connection to Azure Blob", e);
    }
  }

  private int getConnectionResponseCode(String accountname, String accountKey) throws Exception {

    // Generate precise timestamp
    String timestamp = generateTimestamp();

    // Construct canonical resource
    String canonicalResource = "/" + accountname + "/\ncomp:list";

    // Create signing string without line breaks
    String stringToSign = createStringToSign(timestamp, canonicalResource);

    // Generate authorization signature
    String authSignature = generateAuthorizationSignature(stringToSign, accountKey);

    // Construct authorization header
    String authorizationHeader = constructAuthorizationHeader(authSignature, accountname);

    // Execute blob storage list request
    return executeListContainersRequest(timestamp, authorizationHeader, accountname);
  }

  private String generateTimestamp() {
    return DateTimeFormatter
        .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        .withZone(ZoneOffset.UTC)
        .format(Instant.now());
  }

  private String createStringToSign(String timestamp, String canonicalResource) {

    String canonicalizedHeaders = String.format("x-ms-date:%s\nx-ms-version:%s\n", timestamp, API_VERSION);

    return String.format(
        "%s\n" +  // HTTP method (e.g., GET)
            "\n" +     // Content-Encoding
            "\n" +     // Content-Language
            "\n" +     // Content-Length (empty for GET)
            "\n" +     // Content-MD5
            "\n" +     // Content-Type
            "\n" +     // Date (empty because x-ms-date is used)
            "\n" +     // If-Modified-Since
            "\n" +     // If-Match
            "\n" +     // If-None-Match
            "\n" +     // If-Unmodified-Since
            "\n" +     // Range
            "%s" +     // CanonicalizedHeaders
            "%s",      // CanonicalizedResource
        "GET",
        canonicalizedHeaders,
        canonicalResource
    );
  }

  private String generateAuthorizationSignature(String stringToSign, String accountKey) throws Exception {
    byte[] decodedKey = Base64.getDecoder().decode(accountKey);
    Mac hmacSHA256 = Mac.getInstance(SIGNATURE_METHOD);
    SecretKeySpec secretKey = new SecretKeySpec(decodedKey, SIGNATURE_METHOD);
    hmacSHA256.init(secretKey);

    byte[] signatureBytes = hmacSHA256.doFinal(stringToSign.getBytes("UTF-8"));
    return Base64.getEncoder().encodeToString(signatureBytes);
  }

  private String constructAuthorizationHeader(String signature, String accountName) {
    return AUTHORIZATION_SCHEME + " " + accountName + ":" + signature;
  }

  private int executeListContainersRequest(String timestamp, String authorizationHeader, String accountName) throws Exception {
    String endpoint = String.format("https://%s.blob.core.windows.net/?comp=list", accountName);
    URL url = new URL(endpoint);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", authorizationHeader);
    connection.setRequestProperty("x-ms-date", timestamp);
    connection.setRequestProperty("x-ms-version", API_VERSION);

    int responseCode = connection.getResponseCode();
    LOGGER.debug("Response Code: " + responseCode);

    if(responseCode != HttpURLConnection.HTTP_OK) {

      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));

      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();

      LOGGER.debug("Full Response: " + response);
    }

    return responseCode;
  }
}
