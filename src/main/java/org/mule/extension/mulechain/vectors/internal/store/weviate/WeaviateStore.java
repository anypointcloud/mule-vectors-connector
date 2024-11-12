package org.mule.extension.mulechain.vectors.internal.store.weviate;

import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.Config;
import io.weaviate.client.v1.auth.exception.AuthException;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.Get;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.base.Result;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.VectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

import java.util.*;
import java.util.stream.Stream;

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

  public String getIndex() {

    return storeName.substring(0, 1).toUpperCase() + storeName.substring(1);
  }

  public JSONObject listSources() {

    HashMap<String, JSONObject> sourceObjectMap = new HashMap<String, JSONObject>();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(JSON_KEY_STORE_NAME, storeName);

    WeaviateClient weaviateClient;
    try {
      weaviateClient = WeaviateAuthClient.apiKey(new Config(protocol, host), apiKey);
    } catch (AuthException e) {
      // handle error in case of authorization problems
      throw new RuntimeException(e);
    }

    String[] properties = new String[]{"metadata"};
    String cursor = "";

    getBatchWithCursor(weaviateClient, storeName, properties, queryParams.embeddingPageSize(), cursor);

    jsonObject.put(JSON_KEY_SOURCES, JsonUtils.jsonObjectCollectionToJsonArray(sourceObjectMap.values()));
    jsonObject.put(JSON_KEY_SOURCE_COUNT, sourceObjectMap.size());

    return jsonObject;
  }

  private Result<GraphQLResponse> getBatchWithCursor(WeaviateClient client,
                                                     String className, String[] properties, int batchSize, String cursor) {
    Get query = client.graphQL().get()
        .withClassName(className)
        // Optionally retrieve the vector embedding by adding `vector` to the _additional fields
        .withFields(Stream.concat(Arrays.stream(properties), Stream.of("_additional { id vector }"))
                        .map(prop -> Field.builder().name(prop).build())
                        .toArray(Field[]::new)
        )
        .withLimit(batchSize);

    if (cursor != null) {
      return query.withAfter(cursor).run();
    }
    return query.run();
  }

  private List<Map<String, Object>> getProperties(GraphQLResponse result, String className, String[] classProperties) {

    Object get = ((Map<?, ?>) result.getData()).get("Get");
    Object clazz = ((Map<?, ?>) get).get(className);
    List<?> objects = (List<?>) clazz;
    List<Map<String, Object>> res = new ArrayList<>();
    for (Object obj : objects) {
      Map<String, Object> objProps = new HashMap<>();
      for (String prop: classProperties) {
        Object propValue = ((Map<?, ?>) obj).get(prop);
        objProps.put(prop, propValue);
      }
      Object additional = ((Map<?, ?>) obj).get("_additional");
      Object id = ((Map<?, ?>) additional).get("id");
      objProps.put("id", id);
      Object vector = ((Map<?, ?>) additional).get("vector");
      objProps.put("vector", vector);
      res.add(objProps);
    }
    return res;
  }

  private int getObjectsCount(GraphQLResponse result, String className) {

    Object get = ((Map<?, ?>) result.getData()).get("Get");
    Object clazz = ((Map<?, ?>) get).get(className);
    List<?> objects = (List<?>) clazz;
    return objects.size();
  }
}
