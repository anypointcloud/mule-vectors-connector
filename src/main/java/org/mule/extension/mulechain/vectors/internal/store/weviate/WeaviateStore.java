package org.mule.extension.mulechain.vectors.internal.store.weviate;

import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.VectorStore;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class WeaviateStore extends VectorStore {

  private String host;
  private String protocol;
  private String apiKey;

  public WeaviateStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_WEAVIATE);
    this.host = vectorStoreConfig.getString("WEAVIATE_HOST");
    this.protocol = vectorStoreConfig.getString("WEAVIATE_PROTOCOL");
    this.apiKey = vectorStoreConfig.getString("WEAVIATE_APIKEY");
  }
}
