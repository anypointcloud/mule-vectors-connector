package org.mule.extension.mulechain.vectors.internal.helper.store;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.factory.EmbeddingModelFactory;
import org.mule.extension.mulechain.vectors.internal.helper.factory.EmbeddingStoreFactory;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.helper.store.aisearch.AISearchStore;
import org.mule.extension.mulechain.vectors.internal.helper.store.milvus.MilvusStore;
import org.mule.extension.mulechain.vectors.internal.helper.store.pgvector.PGVectorStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

public class VectorStore {

  protected static final Logger LOGGER = LoggerFactory.getLogger(VectorStore.class);

  protected String storeName;
  protected Configuration configuration;
  protected QueryParameters queryParams;
  protected EmbeddingModelNameParameters modelParams;
  protected EmbeddingModel embeddingModel;

  public VectorStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    this.storeName = storeName;
    this.configuration = configuration;
    this.queryParams = queryParams;
    this.modelParams = modelParams;
  }

  public EmbeddingModel embeddingModel() {

    if(this.embeddingModel == null) {
      this.embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);
    }
    return this.embeddingModel;
  }

  public Embedding getZeroVectorEmbedding() {

    // Create a general query vector (e.g., zero vector). Zero vector is often used when you need to retrieve all
    // embeddings without any specific bias.
    float[] queryVector = new float[embeddingModel.dimension()];
    for (int i = 0; i < embeddingModel.dimension(); i++) {
      queryVector[i]=0.0f;  // Zero vector
    }
    return new Embedding(queryVector);
  }

  public JSONObject listSources() {

    dev.langchain4j.store.embedding.EmbeddingStore<TextSegment>
        store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    Embedding queryEmbedding = getZeroVectorEmbedding();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);
    JSONArray sources = new JSONArray();

    List<EmbeddingMatch<TextSegment>> embeddingMatches = null;
    HashMap<String, JSONObject> sourcesJSONObjectHashMap = new HashMap<String, JSONObject>();
    String lowerBoundaryIngestionDateTime = "0000-00-00T00:00:00.000Z";
    int lowerBoundaryIndex = -1;

    LOGGER.debug("Embedding page size: " + queryParams.embeddingPageSize());
    String previousPageEmbeddingId = "";
    do {

      LOGGER.debug("Embedding page filter: lowerBoundaryIngestionDateTime: " + lowerBoundaryIngestionDateTime + ", lowerBoundaryIndex: " + lowerBoundaryIndex);

      Filter
          condition1 = metadataKey(Constants.METADATA_KEY_INGESTION_DATETIME).isGreaterThanOrEqualTo(lowerBoundaryIngestionDateTime);
      Filter condition2 = metadataKey(Constants.METADATA_KEY_INDEX).isGreaterThan(String.valueOf(lowerBoundaryIndex)); // Index must be handled as a String
      Filter condition3 = metadataKey(Constants.METADATA_KEY_INGESTION_DATETIME).isGreaterThan(lowerBoundaryIngestionDateTime);

      Filter searchFilter = (condition1.and(condition2)).or(condition3);

      EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
          .queryEmbedding(queryEmbedding)
          .maxResults(queryParams.embeddingPageSize())
          .minScore(0.0)
          .filter(searchFilter)
          .build();

      EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
      embeddingMatches = searchResult.matches();

      String currentPageEmbeddingId = "";
      LOGGER.debug("Embedding page matches: " + embeddingMatches.size());
      for (EmbeddingMatch<TextSegment> match : embeddingMatches) {

        Metadata matchMetadata = match.embedded().metadata();
        String sourceId = matchMetadata.getString(Constants.METADATA_KEY_SOURCE_ID);
        String index = matchMetadata.getString(Constants.METADATA_KEY_INDEX);
        String fileName = matchMetadata.getString(Constants.METADATA_KEY_FILE_NAME);
        String url = matchMetadata.getString(Constants.METADATA_KEY_URL);
        String fullPath = matchMetadata.getString(Constants.METADATA_KEY_FULL_PATH);
        String absoluteDirectoryPath = matchMetadata.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH);
        String ingestionDatetime = matchMetadata.getString(Constants.METADATA_KEY_INGESTION_DATETIME);

        if(lowerBoundaryIngestionDateTime.compareTo(ingestionDatetime) < 0) {

          lowerBoundaryIngestionDateTime = ingestionDatetime;
          lowerBoundaryIndex = Integer.parseInt(index);
        } else if(lowerBoundaryIngestionDateTime.compareTo(ingestionDatetime) == 0) {

          if(Integer.parseInt(index) > lowerBoundaryIndex) {
            lowerBoundaryIndex = Integer.parseInt(index);
          }
        }

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

            int currentSegmentCount = Integer.parseInt(index) + 1;
            int storedSegmentCount = (int) sourcesJSONObjectHashMap.get(sourceUniqueKey).get("segmentCount");
            if(currentSegmentCount > storedSegmentCount) {

              sourcesJSONObjectHashMap.put(sourceUniqueKey, sourceObject);
            }

          } else {

            sourcesJSONObjectHashMap.put(sourceUniqueKey, sourceObject);
          }
        }
        currentPageEmbeddingId = match.embeddingId();
      }

      if(previousPageEmbeddingId.compareTo(currentPageEmbeddingId) == 0) {
        break;
      } else {
        previousPageEmbeddingId = currentPageEmbeddingId;
      }

    } while(embeddingMatches.size() == queryParams.embeddingPageSize());

    jsonObject.put("sources", JsonUtils.jsonObjectCollectionToJsonArray(sourcesJSONObjectHashMap.values()));
    jsonObject.put("sourceCount", sourcesJSONObjectHashMap.size());
    return jsonObject;
  }

  /**
   * Retrieves a unique key for a given source object by checking specific metadata fields.
   * <p>
   * The method first attempts to retrieve a unique identifier using the source ID (if available).
   * If the source ID is not present, it generates an alternative key by concatenating the
   * {@code fullPath} or {@code url} (whichever is available) with the {@code ingestionDatetime}.
   * </p>
   *
   * @param sourceObject A {@code JSONObject} containing metadata fields for the source. The expected
   *                     keys include {@code METADATA_KEY_SOURCE_ID}, {@code METADATA_KEY_URL},
   *                     {@code METADATA_KEY_FULL_PATH}, and {@code METADATA_KEY_INGESTION_DATETIME}.
   * @return A unique key as a {@code String}. If {@code sourceId} is present, it is returned directly.
   *         Otherwise, the alternative key, based on available fields, is generated and returned.
   *         Returns an empty string if all fields are missing or empty.
   */
  protected String getSourceUniqueKey(JSONObject sourceObject) {

    String sourceId = sourceObject.has(Constants.METADATA_KEY_SOURCE_ID) ? sourceObject.getString(Constants.METADATA_KEY_SOURCE_ID) : "";

    String url = sourceObject.has(Constants.METADATA_KEY_URL) ? sourceObject.getString(Constants.METADATA_KEY_URL) : "";
    String fullPath = sourceObject.has(Constants.METADATA_KEY_FULL_PATH) ? sourceObject.getString(Constants.METADATA_KEY_FULL_PATH) : "";
    String source = sourceObject.has(Constants.METADATA_KEY_SOURCE) ? sourceObject.getString(Constants.METADATA_KEY_SOURCE) : "";
    String ingestionDatetime = sourceObject.has(Constants.METADATA_KEY_INGESTION_DATETIME) ? sourceObject.getString(Constants.METADATA_KEY_INGESTION_DATETIME) : "";

    String alternativeKey =
        ((fullPath != null && !fullPath.isEmpty()) ? fullPath :
            ((url != null && !url.isEmpty()) ? url :
                  (source != null && !source.isEmpty()) ? source : "")) +
              ((ingestionDatetime != null && !ingestionDatetime.isEmpty()) ? ingestionDatetime : "");

    return !sourceId.isEmpty() ? sourceId : alternativeKey;
  }

  protected JSONObject getSourceObject(JSONObject metadataObject) {

    String sourceId = metadataObject.has(Constants.METADATA_KEY_SOURCE_ID) ?  metadataObject.getString(Constants.METADATA_KEY_SOURCE_ID) : null;
    String index = metadataObject.has(Constants.METADATA_KEY_INDEX) ? metadataObject.getString(Constants.METADATA_KEY_INDEX) : null;
    String fileName = metadataObject.has(Constants.METADATA_KEY_FILE_NAME) ?  metadataObject.getString(Constants.METADATA_KEY_FILE_NAME) : null;
    String url = metadataObject.has(Constants.METADATA_KEY_URL) ?  metadataObject.getString(Constants.METADATA_KEY_URL) : null;
    String fullPath = metadataObject.has(Constants.METADATA_KEY_FULL_PATH) ?  metadataObject.getString(Constants.METADATA_KEY_FULL_PATH) : null;
    String source = metadataObject.has(Constants.METADATA_KEY_SOURCE) ?  metadataObject.getString(Constants.METADATA_KEY_SOURCE) : null;
    String absoluteDirectoryPath = metadataObject.has(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH) ?  metadataObject.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH) : null;
    String ingestionDatetime = metadataObject.has(Constants.METADATA_KEY_INGESTION_DATETIME) ?  metadataObject.getString(Constants.METADATA_KEY_INGESTION_DATETIME) : null;

    JSONObject sourceObject = new JSONObject();
    sourceObject.put("segmentCount", Integer.parseInt(index) + 1);
    sourceObject.put(Constants.METADATA_KEY_SOURCE_ID, sourceId);
    sourceObject.put(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
    sourceObject.put(Constants.METADATA_KEY_SOURCE, source);
    sourceObject.put(Constants.METADATA_KEY_FULL_PATH, fullPath);
    sourceObject.put(Constants.METADATA_KEY_FILE_NAME, fileName);
    sourceObject.put(Constants.METADATA_KEY_URL, url);
    sourceObject.put(Constants.METADATA_KEY_INGESTION_DATETIME, ingestionDatetime);

    return sourceObject;
  }

  public static Builder builder() {

    return new Builder();
  }

  public static class Builder {

    private String storeName;
    private Configuration configuration;
    private QueryParameters queryParams;
    private EmbeddingModelNameParameters modelParams;
    private EmbeddingModel embeddingModel;

    public Builder() {

    }

    public Builder storeName(String storeName) {
      this.storeName = storeName;
      return this;
    }

    public Builder configuration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder queryParams(QueryParameters queryParams) {
      this.queryParams = queryParams;
      return this;
    }

    public Builder modelParams(EmbeddingModelNameParameters modelParams) {
      this.modelParams = modelParams;
      return this;
    }

    public Builder embeddingModel(EmbeddingModel embeddingModel) {
      this.embeddingModel = embeddingModel;
      return this;
    }

    public VectorStore build() {

      VectorStore embeddingStore;

      switch (configuration.getVectorStore()) {

        case Constants.VECTOR_STORE_MILVUS:

          embeddingStore = new MilvusStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_PGVECTOR:

          embeddingStore = new PGVectorStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_AI_SEARCH:

          embeddingStore = new AISearchStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_CHROMA:

        case Constants.VECTOR_STORE_PINECONE:

        case Constants.VECTOR_STORE_ELASTICSEARCH:

        case Constants.VECTOR_STORE_WEAVIATE:

          embeddingStore = new VectorStore(storeName, configuration, queryParams, modelParams);
          break;

        default:
          //throw new IllegalOperationException("Unsupported Vector Store: " + configuration.getVectorStore());
          embeddingStore = null;
      }
      return embeddingStore;
    }
  }
}
