package org.mule.extension.vectors.internal.store.elasticsearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.mule.extension.vectors.internal.config.StoreConfiguration;
import org.mule.extension.vectors.internal.connection.store.elasticsearch.ElasticsearchStoreConnection;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;

public class ElasticsearchStore extends BaseStore {

  private final String url;
  private final String user;
  private final String password;
  private final String apiKey;

  private RestClient restClient;

  private RestClient getRestClient() {

    if(this.restClient == null) {

      if (!Utils.isNullOrBlank(user)) {

        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(
            AuthScope.ANY, new UsernamePasswordCredentials(user, password)
        );

        this.restClient = RestClient
            .builder(HttpHost.create(url))
            .setHttpClientConfigCallback(hc -> hc
                .setDefaultCredentialsProvider(credsProv)
            )
            .build();

      } else if (!Utils.isNullOrBlank(apiKey)) {

        this.restClient = RestClient
            .builder(HttpHost.create(url))
            .setDefaultHeaders(new Header[]{
                new BasicHeader("Authorization", "ApiKey " + apiKey)
            })
            .build();
      }
    }
    return restClient;
  }

  public ElasticsearchStore(StoreConfiguration storeConfiguration, ElasticsearchStoreConnection elasticsearchStoreConnection, String storeName, QueryParameters queryParams) {

    super(storeConfiguration, elasticsearchStoreConnection, storeName, queryParams, 0, true);

    this.url = elasticsearchStoreConnection.getUrl();
    this.user = elasticsearchStoreConnection.getUser();
    this.password = elasticsearchStoreConnection.getPassword();
    this.apiKey = elasticsearchStoreConnection.getApiKey();
    this.restClient = elasticsearchStoreConnection.getRestClient();
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return ElasticsearchEmbeddingStore.builder()
        .restClient(getRestClient())
        .indexName(storeName)
        .build();
  }
}
