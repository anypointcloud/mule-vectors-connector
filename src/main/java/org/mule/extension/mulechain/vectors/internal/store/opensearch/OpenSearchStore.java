package org.mule.extension.mulechain.vectors.internal.store.opensearch;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

public class OpenSearchStore extends BaseStore {

  private String url;
  private String userName;
  private String password;

  public OpenSearchStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_OPENSEARCH);
    this.url = vectorStoreConfig.getString("OPENSEARCH_URL");
    this.userName = vectorStoreConfig.getString("OPENSEARCH_USER");
    this.password = vectorStoreConfig.getString("OPENSEARCH_PASSWORD");
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
