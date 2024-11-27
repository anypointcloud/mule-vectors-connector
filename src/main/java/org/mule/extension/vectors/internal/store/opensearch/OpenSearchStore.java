package org.mule.extension.vectors.internal.store.opensearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.extension.vectors.internal.util.JsonUtils;

public class OpenSearchStore extends BaseStore {

  private final String url;
  private final String userName;
  private final String password;

  public OpenSearchStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    OpenSearchStoreConfiguration openSearchStoreConfiguration = (OpenSearchStoreConfiguration) configuration.getStoreConfiguration();
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
