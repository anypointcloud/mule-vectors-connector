package org.mule.extension.vectors.internal.store.elasticsearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.extension.vectors.internal.util.JsonUtils;

public class ElasticsearchStore extends BaseStore {

  private String url;
  private String userName;
  private String password;

  public ElasticsearchStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_ELASTICSEARCH);
    this.url = vectorStoreConfig.getString("ELASTICSEARCH_URL");
    this.userName = vectorStoreConfig.getString("ELASTICSEARCH_USER");
    this.password = vectorStoreConfig.getString("ELASTICSEARCH_PASSWORD");
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
