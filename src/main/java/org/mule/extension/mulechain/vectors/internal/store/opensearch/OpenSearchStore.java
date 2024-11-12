package org.mule.extension.mulechain.vectors.internal.store.opensearch;

import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.VectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

public class OpenSearchStore extends VectorStore {

  private String url;
  private String userName;
  private String password;

  public OpenSearchStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_OPENSEARCH);
    this.url = vectorStoreConfig.getString("OPENSEARCH_URL");
    this.userName = vectorStoreConfig.getString("OPENSEARCH_USER");
    this.password = vectorStoreConfig.getString("OPENSEARCH_PASSWORD");
  }
}
