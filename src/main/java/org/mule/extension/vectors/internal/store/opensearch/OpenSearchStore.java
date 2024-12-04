package org.mule.extension.vectors.internal.store.opensearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;

public class OpenSearchStore extends BaseStore {

  private final String url;
  private final String userName;
  private final String password;

  public OpenSearchStore(String storeName, CompositeConfiguration compositeConfiguration, QueryParameters queryParams, int dimension) {

    super(storeName, compositeConfiguration, queryParams, dimension);

    OpenSearchStoreConfiguration openSearchStoreConfiguration = (OpenSearchStoreConfiguration) compositeConfiguration.getStoreConfiguration();
    this.url = openSearchStoreConfiguration.getUrl();
    this.userName = openSearchStoreConfiguration.getUserName();
    this.password = openSearchStoreConfiguration.getPassword();
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return OpenSearchEmbeddingStore.builder()
        .serverUrl(url)
        .userName(userName)
        .password(password)
        .indexName(storeName)
        .build();
  }
}
