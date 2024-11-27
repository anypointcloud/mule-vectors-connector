package org.mule.extension.vectors.internal.store.pinecone;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.extension.vectors.internal.util.JsonUtils;

public class PineconeStore extends BaseStore {

  private String apiKey;
  private String cloud;
  private String region;

  public PineconeStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    PineconeStoreConfiguration pineconeStoreConfiguration = (PineconeStoreConfiguration) configuration.getStoreConfiguration();
    this.apiKey = pineconeStoreConfiguration.getApiKey();
    this.cloud = pineconeStoreConfiguration.getCloud();
    this.region = pineconeStoreConfiguration.getRegion();
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return PineconeEmbeddingStore.builder()
        .apiKey(apiKey)
        .index(storeName)
        .nameSpace("ns0mc_" + storeName)
        .createIndex(PineconeServerlessIndexConfig.builder()
                         .cloud(cloud)
                         .region(region)
                         .dimension(dimension)
                         .build())
        .build();
  }
}
