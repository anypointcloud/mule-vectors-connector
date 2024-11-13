package org.mule.extension.mulechain.vectors.internal.store;

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
import org.mule.extension.mulechain.vectors.internal.store.aisearch.AISearchStore;
import org.mule.extension.mulechain.vectors.internal.store.chroma.ChromaStore;
import org.mule.extension.mulechain.vectors.internal.store.elasticsearch.ElasticsearchStore;
import org.mule.extension.mulechain.vectors.internal.store.milvus.MilvusStore;
import org.mule.extension.mulechain.vectors.internal.store.opensearch.OpenSearchStore;
import org.mule.extension.mulechain.vectors.internal.store.pgvector.PGVectorStore;
import org.mule.extension.mulechain.vectors.internal.store.pinecone.PineconeStore;
import org.mule.extension.mulechain.vectors.internal.store.weviate.WeaviateStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * The {@code VectorStore} class provides a framework for interacting with various types of vector stores,
 * enabling storage and retrieval of vector embeddings for data analysis and retrieval purposes. It serves as
 * an abstract base for specific implementations such as Milvus, PGVector, and AI Search stores.
 */
public class VectorStore {

  protected static final Logger LOGGER = LoggerFactory.getLogger(VectorStore.class);

  protected static final String JSON_KEY_SOURCES = "sources";
  protected static final String JSON_KEY_SEGMENT_COUNT = "segmentCount";
  protected static final String JSON_KEY_SOURCE_COUNT = "sourceCount";
  protected static final String JSON_KEY_STORE_NAME = "storeName";

  protected String storeName;
  protected Configuration configuration;
  protected QueryParameters queryParams;
  protected EmbeddingModelNameParameters modelParams;
  protected EmbeddingModel embeddingModel;

  /**
   * Constructs a new {@code VectorStore} instance with specified configurations.
   *
   * @param storeName    the name of the vector store
   * @param configuration the configuration object containing settings for the vector store
   * @param queryParams  parameters for querying the vector store
   * @param modelParams  parameters for selecting and configuring the embedding model
   */
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

  /**
   * Retrieves the embedding model used by this vector store. Initializes the model if it is not already set.
   *
   * @return the embedding model used by the vector store
   */
  public Embedding getZeroVectorEmbedding() {

    // Create a general query vector (e.g., zero vector). Zero vector is often used when you need to retrieve all
    // embeddings without any specific bias.
    float[] queryVector = new float[embeddingModel().dimension()];
    for (int i = 0; i < embeddingModel().dimension(); i++) {
      queryVector[i]=0.0f;  // Zero vector
    }
    return new Embedding(queryVector);
  }

  /**
   * Retrieves a JSON object listing sources available in the vector store, including metadata for each source.
   *
   * @return a JSON object containing a list of sources and their metadata
   */
  public JSONObject listSources() {

    dev.langchain4j.store.embedding.EmbeddingStore<TextSegment>
        store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    Embedding queryEmbedding = getZeroVectorEmbedding();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(JSON_KEY_STORE_NAME, storeName);
    JSONArray sources = new JSONArray();

    List<EmbeddingMatch<TextSegment>> embeddingMatches = null;
    HashMap<String, JSONObject> sourceObjectMap = new HashMap<String, JSONObject>();
    String lowerBoundaryIngestionDateTime = "0000-00-00T00:00:00.000Z";
    int lowerBoundaryIndex = -1;

    LOGGER.debug("Embedding page size: " + queryParams.embeddingPageSize());
    String previousPageEmbeddingId = "";
    do {

      LOGGER.debug("Embedding page filter: lowerBoundaryIngestionDateTime: " + lowerBoundaryIngestionDateTime + ", lowerBoundaryIndex: " + lowerBoundaryIndex);

      Filter condition1 = metadataKey(Constants.METADATA_KEY_INGESTION_DATETIME).isGreaterThanOrEqualTo(lowerBoundaryIngestionDateTime);
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
        sourceObject.put(JSON_KEY_SEGMENT_COUNT, Integer.parseInt(index) + 1);
        sourceObject.put(Constants.METADATA_KEY_SOURCE_ID, sourceId);
        sourceObject.put(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
        sourceObject.put(Constants.METADATA_KEY_FULL_PATH, fullPath);
        sourceObject.put(Constants.METADATA_KEY_FILE_NAME, fileName);
        sourceObject.put(Constants.METADATA_KEY_URL, url);
        sourceObject.put(Constants.METADATA_KEY_INGESTION_DATETIME, ingestionDatetime);

        addOrUpdateSourceObjectIntoSourceObjectMap(sourceObjectMap, sourceObject);

        currentPageEmbeddingId = match.embeddingId();
      }

      if(previousPageEmbeddingId.compareTo(currentPageEmbeddingId) == 0) {
        break;
      } else {
        previousPageEmbeddingId = currentPageEmbeddingId;
      }

    } while(embeddingMatches.size() == queryParams.embeddingPageSize());

    jsonObject.put(JSON_KEY_SOURCES, JsonUtils.jsonObjectCollectionToJsonArray(sourceObjectMap.values()));
    jsonObject.put(JSON_KEY_SOURCE_COUNT, sourceObjectMap.size());
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

  /**
   * Adds or updates a source object into the source object map.
   *
   * @param sourceObjectMap The map of source objects keyed by their unique keys.
   * @param sourceObject    The source object to add or update.
   */
  protected void addOrUpdateSourceObjectIntoSourceObjectMap(HashMap<String, JSONObject> sourceObjectMap, JSONObject sourceObject) {

    String sourceUniqueKey = getSourceUniqueKey(sourceObject);

    // Add sourceObject to sources only if it has at least one key-value pair and it's possible to generate a key
    if (!sourceObject.isEmpty() && sourceUniqueKey != null && !sourceUniqueKey.isEmpty()) {
      // Overwrite sourceObject if current one has a greater index (greatest index represents the number of segments)
      if(sourceObjectMap.containsKey(sourceUniqueKey)){
        // Get current index
        int currentSegmentCount = sourceObject.getInt(JSON_KEY_SEGMENT_COUNT);
        // Get previously stored index
        int storedSegmentCount = (int) sourceObjectMap.get(sourceUniqueKey).get(JSON_KEY_SEGMENT_COUNT);
        // Check if object need to be updated
        if(currentSegmentCount > storedSegmentCount) {
          sourceObjectMap.put(sourceUniqueKey, sourceObject);
        }
      } else {
        sourceObjectMap.put(sourceUniqueKey, sourceObject);
      }
    }
  }

  /**
   * Extracts and organizes metadata fields from a given JSON object to create a structured source object.
   * <p>
   * The generated source object includes keys such as source ID, file name, URL, full path, and ingestion
   * datetime, among others.
   * </p>
   *
   * @param metadataObject a {@code JSONObject} containing metadata fields.
   * @return a {@code JSONObject} with organized metadata for a source.
   */
  protected JSONObject getSourceObject(JSONObject metadataObject) {

    String sourceId = metadataObject.has(Constants.METADATA_KEY_SOURCE_ID) ?  metadataObject.getString(Constants.METADATA_KEY_SOURCE_ID) : null;
    String index = metadataObject.has(Constants.METADATA_KEY_INDEX) ? metadataObject.getString(Constants.METADATA_KEY_INDEX) : null;
    String fileName = metadataObject.has(Constants.METADATA_KEY_FILE_NAME) ?  metadataObject.getString(Constants.METADATA_KEY_FILE_NAME) : null;
    String url = metadataObject.has(Constants.METADATA_KEY_URL) ?  metadataObject.getString(Constants.METADATA_KEY_URL) : null;
    String fullPath = metadataObject.has(Constants.METADATA_KEY_FULL_PATH) ?  metadataObject.getString(Constants.METADATA_KEY_FULL_PATH) : null;
    String source = metadataObject.has(Constants.METADATA_KEY_SOURCE) ?  metadataObject.getString(Constants.METADATA_KEY_SOURCE) : null;
    String absoluteDirectoryPath = metadataObject.has(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH) ?  metadataObject.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH) : null;
    String ingestionDatetime = metadataObject.has(Constants.METADATA_KEY_INGESTION_DATETIME) ?  metadataObject.getString(Constants.METADATA_KEY_INGESTION_DATETIME) : null;
    Long ingestionTimestamp = metadataObject.has(Constants.METADATA_KEY_INGESTION_TIMESTAMP) ?  metadataObject.getLong(Constants.METADATA_KEY_INGESTION_TIMESTAMP) : null;

    JSONObject sourceObject = new JSONObject();
    sourceObject.put(JSON_KEY_SEGMENT_COUNT, Integer.parseInt(index) + 1);
    sourceObject.put(Constants.METADATA_KEY_SOURCE_ID, sourceId);
    sourceObject.put(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
    sourceObject.put(Constants.METADATA_KEY_SOURCE, source);
    sourceObject.put(Constants.METADATA_KEY_FULL_PATH, fullPath);
    sourceObject.put(Constants.METADATA_KEY_FILE_NAME, fileName);
    sourceObject.put(Constants.METADATA_KEY_URL, url);
    sourceObject.put(Constants.METADATA_KEY_INGESTION_DATETIME, ingestionDatetime);
    sourceObject.put(Constants.METADATA_KEY_INGESTION_TIMESTAMP, ingestionTimestamp);

    return sourceObject;
  }

  /**
   * Provides a {@link Builder} instance for configuring and creating {@code VectorStore} objects.
   * <p>
   * The builder pattern allows for more flexible and readable configuration of a {@code VectorStore}.
   * Use this to set parameters such as the store name, configuration, query parameters, and embedding model.
   * </p>
   *
   * @return a new {@code Builder} instance.
   */
  public static Builder builder() {

    return new Builder();
  }

  /**
   * Builder class for creating instances of {@link VectorStore}.
   * <p>
   * The {@code Builder} class allows you to set various configuration parameters before
   * creating a {@code VectorStore} instance. These parameters include the store name,
   * configuration settings, query parameters, and embedding model details.
   * </p>
   */
  public static class Builder {

    private String storeName;
    private Configuration configuration;
    private QueryParameters queryParams;
    private EmbeddingModelNameParameters modelParams;
    private EmbeddingModel embeddingModel;

    public Builder() {

    }

    /**
     * Sets the store name for the {@code VectorStore}.
     *
     * @param storeName the name of the vector store.
     * @return the {@code Builder} instance, for method chaining.
     */
    public Builder storeName(String storeName) {
      this.storeName = storeName;
      return this;
    }

    /**
     * Sets the configuration for the {@code VectorStore}.
     *
     * @param configuration the configuration parameters.
     * @return the {@code Builder} instance, for method chaining.
     */
    public Builder configuration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    /**
     * Sets the query parameters for embedding searches.
     *
     * @param queryParams the query parameters to use.
     * @return the {@code Builder} instance, for method chaining.
     */
    public Builder queryParams(QueryParameters queryParams) {
      this.queryParams = queryParams;
      return this;
    }

    /**
     * Sets the parameters for the embedding model.
     *
     * @param modelParams parameters for selecting and configuring the embedding model.
     * @return the {@code Builder} instance, for method chaining.
     */
    public Builder modelParams(EmbeddingModelNameParameters modelParams) {
      this.modelParams = modelParams;
      return this;
    }

    /**
     * Sets a pre-configured embedding model for the {@code VectorStore}.
     *
     * @param embeddingModel the embedding model to use.
     * @return the {@code Builder} instance, for method chaining.
     */
    public Builder embeddingModel(EmbeddingModel embeddingModel) {
      this.embeddingModel = embeddingModel;
      return this;
    }

    /**
     * Builds and returns a new {@link VectorStore} instance based on the builder's configuration.
     * <p>
     * Depending on the specified configuration, it returns an instance of the appropriate
     * store class (e.g., {@link MilvusStore}, {@link PGVectorStore}, or {@link AISearchStore}).
     * If no matching store configuration is found, it returns a default {@code VectorStore} instance.
     * </p>
     *
     * @return a {@code VectorStore} instance.
     * @throws IllegalArgumentException if the configured vector store is unsupported.
     */
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

        case Constants.VECTOR_STORE_WEAVIATE:

          embeddingStore = new WeaviateStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_CHROMA:

          embeddingStore = new ChromaStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_PINECONE:

          embeddingStore = new PineconeStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_ELASTICSEARCH:

          embeddingStore = new ElasticsearchStore(storeName, configuration, queryParams, modelParams);
          break;

        case Constants.VECTOR_STORE_OPENSEARCH:

          embeddingStore = new OpenSearchStore(storeName, configuration, queryParams, modelParams);
          break;

        default:
          //throw new IllegalOperationException("Unsupported Vector Store: " + configuration.getVectorStore());
          embeddingStore = null;
      }
      return embeddingStore;
    }
  }
}
