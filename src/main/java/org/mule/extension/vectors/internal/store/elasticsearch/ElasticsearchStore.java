package org.mule.extension.vectors.internal.store.elasticsearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfiguration;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.extension.vectors.internal.util.JsonUtils;

public class ElasticsearchStore extends BaseStore {

  private final String url;
  private final String userName;
  private final String password;

  public ElasticsearchStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    ElasticsearchStoreConfiguration elasticsearchStoreConfiguration = (ElasticsearchStoreConfiguration) configuration.getStoreConfiguration();
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
