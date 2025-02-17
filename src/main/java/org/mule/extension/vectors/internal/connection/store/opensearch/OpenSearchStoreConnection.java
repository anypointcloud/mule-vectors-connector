package org.mule.extension.vectors.internal.connection.store.opensearch;

import dev.langchain4j.internal.Utils;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

public class OpenSearchStoreConnection implements BaseStoreConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchStoreConnection.class);

  private String url;
  private String user;
  private String password;
  private String apiKey;
  
  private OpenSearchClient openSearchClient;

  public OpenSearchStoreConnection(String url, String userName, String password, String apikey) {
    this.url = url;
    this.user = userName;
    this.password = password;
    this.apiKey = apikey;
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getApiKey() {
    return apiKey;
  }

  public OpenSearchClient getOpenSearchClient() {
    return openSearchClient;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_OPENSEARCH;
  }

  @Override
  public void connect() throws ConnectionException {

    try {

      org.apache.hc.core5.http.HttpHost openSearchHost = org.apache.hc.core5.http.HttpHost.create(url);

      OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost[]{openSearchHost}).setMapper(new JacksonJsonpMapper()).setHttpClientConfigCallback((httpClientBuilder) -> {

        if (!Utils.isNullOrBlank(apiKey)) {
          httpClientBuilder.setDefaultHeaders(Collections.singletonList(new BasicHeader("Authorization", "ApiKey " + apiKey)));
        }

        if (!Utils.isNullOrBlank(user) && !Utils.isNullOrBlank(password)) {
          org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
              credentialsProvider = new org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider();
          credentialsProvider.setCredentials(new org.apache.hc.client5.http.auth.AuthScope(openSearchHost), new org.apache.hc.client5.http.auth.UsernamePasswordCredentials(user, password.toCharArray()));
          httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        httpClientBuilder.setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create().build());
        return httpClientBuilder;
      }).build();

      this.openSearchClient = new OpenSearchClient(transport);

      if(!openSearchClient.ping().value()) {

        throw new ConnectionException("Impossible to connect to OpenSearch. Ping to the service failed.");
      }

    } catch (ConnectionException e) {

      throw e;
    } catch (Exception e) {

      throw new ConnectionException("Impossible to connect to OpenSearch.", e);
    }
  }

  @Override
  public void disconnect() {

    try {
      // Add logic here

    } catch (Exception e) {

      LOGGER.error("Failed to close connection to OpenSearch.", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isValid() {

    try {

      return openSearchClient.ping().value();

    } catch (IOException e) {

      LOGGER.error("Failed to validate connection to OpenSearch.", e);
      return false;
    }
  }
}
