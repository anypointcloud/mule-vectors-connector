package org.mule.extension.mulechain.vectors.internal.store.chroma;

import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.VectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

public class ChromaStore extends VectorStore {

  private String url;

  public ChromaStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_ELASTICSEARCH);
    this.url = vectorStoreConfig.getString("CHROMA_URL");
  }

}
