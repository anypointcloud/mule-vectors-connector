package org.mule.extension.mulechain.vectors.internal.store.weaviate;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class WeaviateStore extends BaseStore {

  private String host;
  private String protocol;
  private String apiKey;

  public WeaviateStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_WEAVIATE);
    this.host = vectorStoreConfig.getString("WEAVIATE_HOST");
    this.protocol = vectorStoreConfig.getString("WEAVIATE_PROTOCOL");
    this.apiKey = vectorStoreConfig.getString("WEAVIATE_APIKEY");
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return WeaviateEmbeddingStore.builder()
        .scheme(protocol)
        .host(host)
        // "Default" class is used if not specified. Must start from an uppercase letter!
        .objectClass(storeName)
        // If true (default), then WeaviateEmbeddingStore will generate a hashed ID based on provided
        // text segment, which avoids duplicated entries in DB. If false, then random ID will be generated.
        .avoidDups(true)
        // Consistency level: ONE, QUORUM (default) or ALL.
        .consistencyLevel("ALL")
        .apiKey(apiKey)
        .build();
  }
}
