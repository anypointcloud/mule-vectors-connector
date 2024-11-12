package org.mule.extension.mulechain.vectors.internal.store.weviate;

import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.VectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

import java.util.HashMap;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class WeviateStore extends VectorStore {

  private String host;
  private String protocol;
  private String apiKey;

  public WeviateStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_WEAVIATE);
    this.host = vectorStoreConfig.getString("WEAVIATE_HOST");
    this.protocol = vectorStoreConfig.getString("WEAVIATE_PROTOCOL");
    this.apiKey = vectorStoreConfig.getString("WEAVIATE_APIKEY");
  }

  public String getIndex() {

    return storeName.substring(0, 1).toUpperCase() + storeName.substring(1);
  }

  public JSONObject listSources() {

    HashMap<String, JSONObject> sourceObjectMap = new HashMap<String, JSONObject>();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(JSON_KEY_STORE_NAME, storeName);


    jsonObject.put(JSON_KEY_SOURCES, JsonUtils.jsonObjectCollectionToJsonArray(sourceObjectMap.values()));
    jsonObject.put(JSON_KEY_SOURCE_COUNT, sourceObjectMap.size());

    return jsonObject;
  }
}
