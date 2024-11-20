package org.mule.extension.mulechain.vectors.internal.store.qdrant;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

public class QdrantStore extends BaseStore {

  private String host;
  private String collectionName;
  private String apiKey;
  private int port;
  private boolean useTls;
  private String payloadTextKey;

  public QdrantStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_QDRANT);
    this.host = vectorStoreConfig.getString("QDRANT_HOST");
    this.apiKey = vectorStoreConfig.getString("QDRANT_API_KEY");
    this.port = vectorStoreConfig.getInt("QDRANT_GRPC_PORT");
    this.useTls = vectorStoreConfig.getBoolean("QDRANT_USE_TLS");
    this.payloadTextKey = vectorStoreConfig.getString("QDRANT_TEXT_KEY");
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return QdrantEmbeddingStore.builder()
        .host(host)
        .apiKey(apiKey)
        .collectionName(storeName)
        .port(port)
        .useTls(useTls)
        .payloadTextKey(payloadTextKey)
        .build();
  }
}
