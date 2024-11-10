package org.mule.extension.mulechain.vectors.internal.helper.store.milvus;

import com.google.gson.JsonObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.QueryResults;
import io.milvus.orm.iterator.QueryIterator;
import io.milvus.param.ConnectParam;
import io.milvus.param.dml.QueryIteratorParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.R;
import io.milvus.param.collection.LoadCollectionParam;
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

public class MilvusVectorStore extends VectorStore {

  private String uri;

  public MilvusVectorStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_MILVUS);
    this.uri = vectorStoreConfig.getString("MILVUS_URL");
  }

  public JSONObject listSources() {

    HashMap<String, JSONObject> sourcesJSONObjectHashMap = new HashMap<String, JSONObject>();
    
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);
    JSONArray sources = new JSONArray();

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

          LOGGER.debug("Number of segments in current batch: " + batchResults.size());
          for (QueryResultsWrapper.RowRecord rowRecord : batchResults) {

            JsonObject gsonObject = (JsonObject)rowRecord.getFieldValues().get("metadata");
            LOGGER.debug(gsonObject.toString());
            JSONObject metadata = new JSONObject(gsonObject.toString());

            String sourceId = metadata.has(Constants.METADATA_KEY_SOURCE_ID) ?  metadata.getString(Constants.METADATA_KEY_SOURCE_ID) : null;
            String index = metadata.has(Constants.METADATA_KEY_INDEX) ? metadata.getString(Constants.METADATA_KEY_INDEX) : null;
            String fileName = metadata.has(Constants.METADATA_KEY_FILE_NAME) ?  metadata.getString(Constants.METADATA_KEY_FILE_NAME) : null;
            String url = metadata.has(Constants.METADATA_KEY_URL) ?  metadata.getString(Constants.METADATA_KEY_URL) : null;
            String fullPath = metadata.has(Constants.METADATA_KEY_FULL_PATH) ?  metadata.getString(Constants.METADATA_KEY_FULL_PATH) : null;
            String absoluteDirectoryPath = metadata.has(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH) ?  metadata.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH) : null;
            String ingestionDatetime = metadata.has(Constants.METADATA_KEY_INGESTION_DATETIME) ?  metadata.getString(Constants.METADATA_KEY_INGESTION_DATETIME) : null;

            JSONObject sourceObject = new JSONObject();
            sourceObject.put("segmentCount", Integer.parseInt(index) + 1);
            sourceObject.put(Constants.METADATA_KEY_SOURCE_ID, sourceId);
            sourceObject.put(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
            sourceObject.put(Constants.METADATA_KEY_FULL_PATH, fullPath);
            sourceObject.put(Constants.METADATA_KEY_FILE_NAME, fileName);
            sourceObject.put(Constants.METADATA_KEY_URL, url);
            sourceObject.put(Constants.METADATA_KEY_INGESTION_DATETIME, ingestionDatetime);

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
