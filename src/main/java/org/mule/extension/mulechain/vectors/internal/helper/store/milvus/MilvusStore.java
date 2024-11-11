package org.mule.extension.mulechain.vectors.internal.helper.store.milvus;

import com.google.gson.JsonObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.orm.iterator.QueryIterator;
import io.milvus.param.ConnectParam;
import io.milvus.param.dml.QueryIteratorParam;
import io.milvus.param.R;
import io.milvus.response.QueryResultsWrapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.helper.store.VectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class MilvusStore extends VectorStore {

  private String uri;

  public MilvusStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_MILVUS);
    this.uri = vectorStoreConfig.getString("MILVUS_URL");
  }

  public JSONObject listSources() {

    HashMap<String, JSONObject> sourcesJSONObjectHashMap = new HashMap<String, JSONObject>();
    
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);

    // Specify the host and port for the Milvus server
    ConnectParam connectParam = ConnectParam.newBuilder()
        .withUri(this.uri)
        .build();

    MilvusServiceClient client = new MilvusServiceClient(connectParam);

    try {

      boolean hasMore = true;

      // Build the query with iterator
      QueryIteratorParam iteratorParam = QueryIteratorParam.newBuilder()
          .withCollectionName(storeName)
          .withBatchSize((long)queryParams.embeddingPageSize())
          .withOutFields(Arrays.asList("metadata"))
          .build();

      R<QueryIterator> queryIteratorRes = client.queryIterator(iteratorParam);

      if (queryIteratorRes.getStatus() != R.Status.Success.getCode()) {
        System.err.println(queryIteratorRes.getMessage());
      }

      QueryIterator queryIterator = queryIteratorRes.getData();
      List<QueryResultsWrapper.RowRecord> results = new ArrayList<>();

      while (hasMore) {

        List<QueryResultsWrapper.RowRecord> batchResults = queryIterator.next();

        if (batchResults.isEmpty()) {

          queryIterator.close();
          hasMore = false;
        } else {

          for (QueryResultsWrapper.RowRecord rowRecord : batchResults) {

            JsonObject gsonObject = (JsonObject)rowRecord.getFieldValues().get("metadata");
            JSONObject metadataObject = new JSONObject(gsonObject.toString());

            String index = metadataObject.has(Constants.METADATA_KEY_INDEX) ? metadataObject.getString(Constants.METADATA_KEY_INDEX) : null;
            JSONObject sourceObject = getSourceObject(metadataObject);

            String sourceUniqueKey = getSourceUniqueKey(sourceObject);

            // Add sourceObject to sources only if it has at least one key-value pair and it's possible to generate a key
            if (!sourceObject.isEmpty() && sourceUniqueKey != null && !sourceUniqueKey.isEmpty()) {
              // Overwrite sourceObject if current one has a greater index (greatest index represents the number of segments)
              if(sourcesJSONObjectHashMap.containsKey(sourceUniqueKey)){
                // Get current index
                int currentSegmentCount = Integer.parseInt(index) + 1;
                // Get previously stored index
                int storedSegmentCount = (int) sourcesJSONObjectHashMap.get(sourceUniqueKey).get("segmentCount");
                // Check if object need to be updated
                if(currentSegmentCount > storedSegmentCount) {
                  sourcesJSONObjectHashMap.put(sourceUniqueKey, sourceObject);
                }
              } else {
                sourcesJSONObjectHashMap.put(sourceUniqueKey, sourceObject);
              }
            }

          }
        }
      }
    } finally {
      client.close();
    }

    jsonObject.put("sources", JsonUtils.jsonObjectCollectionToJsonArray(sourcesJSONObjectHashMap.values()));
    jsonObject.put("sourceCount", sourcesJSONObjectHashMap.size());

    return jsonObject;
  }
}
