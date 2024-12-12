package org.mule.extension.vectors.internal.store.opensearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.mule.extension.vectors.internal.config.StoreConfiguration;
import org.mule.extension.vectors.internal.connection.store.opensearch.OpenSearchStoreConnection;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import java.net.URISyntaxException;
import java.util.Collections;

public class OpenSearchStore extends BaseStore {

  private final String url;
  private final String user;
  private final String password;
  private final String apiKey;
  private OpenSearchClient openSearchClient;

  public OpenSearchClient getOpenSearchClient() {

    if(openSearchClient == null){

      HttpHost openSearchHost = null;
      try { openSearchHost = HttpHost.create(url); } catch (URISyntaxException e) {

        throw new RuntimeException(e);
      }
      HttpHost finalOpenSearchHost = openSearchHost;
      OpenSearchTransport
          transport = ApacheHttpClient5TransportBuilder.builder(new HttpHost[]{openSearchHost}).setMapper(new JacksonJsonpMapper()).setHttpClientConfigCallback((httpClientBuilder) -> {

        if (!Utils.isNullOrBlank(apiKey)) {
          httpClientBuilder.setDefaultHeaders(Collections.singletonList(new BasicHeader("Authorization", "ApiKey " + apiKey)));
        }

        if (!Utils.isNullOrBlank(user) && !Utils.isNullOrBlank(password)) {
          org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
              credentialsProvider = new org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider();
          credentialsProvider.setCredentials(new org.apache.hc.client5.http.auth.AuthScope(finalOpenSearchHost), new org.apache.hc.client5.http.auth.UsernamePasswordCredentials(user, password.toCharArray()));
          httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        httpClientBuilder.setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create().build());
        return httpClientBuilder;
      }).build();

      this.openSearchClient = new OpenSearchClient(transport);
    }
    return openSearchClient;
  }

  public OpenSearchStore(StoreConfiguration storeConfiguration, OpenSearchStoreConnection openSearchStoreConnection, String storeName, QueryParameters queryParams, int dimension) {

    super(storeConfiguration, openSearchStoreConnection, storeName, queryParams, dimension);

    this.url = openSearchStoreConnection.getUrl();
    this.user = openSearchStoreConnection.getUser();
    this.password = openSearchStoreConnection.getPassword();
    this.apiKey = openSearchStoreConnection.getApiKey();
    this.openSearchClient = openSearchStoreConnection.getOpenSearchClient();
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

      return OpenSearchEmbeddingStore.builder()
          .serverUrl(url)
          .openSearchClient(openSearchClient)
          .indexName(storeName)
          .build();
  }
}
