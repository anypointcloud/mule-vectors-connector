package org.mule.extension.mulechain.vectors.internal.store.pinecone;

import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.VectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

public class PineconeStore extends VectorStore {

  private String apiKey;
  private String cloud;
  private String cloudRegion;

  public PineconeStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_PINECONE);
    this.apiKey = vectorStoreConfig.getString("PINECONE_APIKEY");
    this.cloud = vectorStoreConfig.getString("PINECONE_SERVERLESS_CLOUD");
    this.cloudRegion = vectorStoreConfig.getString("PINECONE_SERVERLESS_REGION");
  }
}
