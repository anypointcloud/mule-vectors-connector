package org.mule.extension.mulechain.vectors.internal.store.milvus;

import com.google.gson.JsonObject;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.orm.iterator.QueryIterator;
import io.milvus.param.ConnectParam;
import io.milvus.param.dml.QueryIteratorParam;
import io.milvus.param.R;
import io.milvus.response.QueryResultsWrapper;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class MilvusStore extends BaseStore {

  private String uri;

  public MilvusStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_MILVUS);
    this.uri = vectorStoreConfig.getString("MILVUS_URL");
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return MilvusEmbeddingStore.builder()
        .uri(uri)
        .collectionName(storeName)
        .dimension(dimension)
        .build();
  }

  public JSONObject listSources() {

    HashMap<String, JSONObject> sourceObjectMap = new HashMap<String, JSONObject>();
    
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(Constants.JSON_KEY_STORE_NAME, storeName);

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
          .withOutFields(Arrays.asList(Constants.STORE_SCHEMA_METADATA_FIELD_NAME))
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

            JsonObject gsonObject = (JsonObject)rowRecord.getFieldValues().get(Constants.STORE_SCHEMA_METADATA_FIELD_NAME);
            JSONObject metadataObject = new JSONObject(gsonObject.toString());
            JSONObject sourceObject = getSourceObject(metadataObject);
            addOrUpdateSourceObjectIntoSourceObjectMap(sourceObjectMap, sourceObject);
          }
        }
      }
    } finally {
      client.close();
    }

    jsonObject.put(Constants.JSON_KEY_SOURCES, JsonUtils.jsonObjectCollectionToJsonArray(sourceObjectMap.values()));
    jsonObject.put(Constants.JSON_KEY_SOURCE_COUNT, sourceObjectMap.size());

    return jsonObject;
  }
}
