package org.mule.extension.vectors.internal.store.elasticsearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;

public class ElasticsearchStore extends BaseStore {

  private final String url;
  private final String userName;
  private final String password;

  public ElasticsearchStore(String storeName, CompositeConfiguration compositeConfiguration, QueryParameters queryParams, int dimension) {

    super(storeName, compositeConfiguration, queryParams, dimension);

    ElasticsearchStoreConfiguration elasticsearchStoreConfiguration = (ElasticsearchStoreConfiguration) compositeConfiguration.getStoreConfiguration();
    this.url = elasticsearchStoreConfiguration.getUrl();
    this.userName = elasticsearchStoreConfiguration.getUserName();
    this.password = elasticsearchStoreConfiguration.getPassword();
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return ElasticsearchEmbeddingStore.builder()
        .serverUrl(url)
        .userName(userName)
        .password(password)
        .indexName(storeName)
        .dimension(dimension)
        .build();
  }
}
