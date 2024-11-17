package org.mule.extension.mulechain.vectors.internal.operation;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.util.List;
import java.nio.charset.StandardCharsets;

import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.EmbeddingOperationValidator;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.*;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.model.BaseModel;
import org.mule.extension.mulechain.vectors.internal.storage.BaseStorage;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.*;

import static java.util.stream.Collectors.joining;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class EmbeddingOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingOperations.class);

  /**
   * Adds Text to Embedding Store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-add-text-to-store")
  public InputStream addTextToStore(  @Config Configuration configuration,
                                      @Alias("text") @DisplayName("Text") String text,
                                      @Alias("storeName") @DisplayName("Store Name")  String storeName,
                                      @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .dimension(embeddingModel.dimension())
        .build();

    EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

    TextSegment textSegment = TextSegment.from(text);
    Embedding textEmbedding = embeddingModel.embed(textSegment).content();
    embeddingStore.add(textEmbedding, textSegment);
    
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("status", Constants.OPERATION_STATUS_ADDED);
    jsonObject.put("textSegment", textSegment.toString());
    jsonObject.put("textEmbedding", textEmbedding.toString());
    jsonObject.put("storeName", storeName);

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

   /**
   * Adds Text to Embedding Store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-generate-from-text")
  public InputStream generateEmbedding( @Config Configuration configuration,
                                        @Alias("text") @DisplayName("Text")  String text,
                                        @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    TextSegment textSegment = TextSegment.from(text);
    Embedding textEmbedding = embeddingModel.embed(textSegment).content();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Segment", textSegment.toString());
    jsonObject.put("Embedding", textEmbedding.toString());
    jsonObject.put("Dimension", textEmbedding.dimension());


    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

  /**
   * Loads multiple files from a folder into the embedding store. URLs are not supported with this operation.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-add-folder-to-store")
  public InputStream addFolderToStore(@Config Configuration configuration,
                                      @Alias("storeName") @DisplayName("Store Name") String storeName,
                                      @ParameterGroup(name = "Documents")  DocumentParameters documentParameters,
                                      @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                                      @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_STORE_METADATA,configuration.getVectorStore());

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .dimension(embeddingModel.dimension())
        .build();

    EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

    EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChar(), segmentationParameters.getMaxOverlapSizeInChars()))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build();

    BaseStorage baseStorage = BaseStorage.builder()
        .configuration(configuration)
        .storageType(documentParameters.getStorageType())
        .contextPath(documentParameters.getContextPath())
        .fileType(documentParameters.getFileType())
        .build();

    long documentNumber = 0;
    while(baseStorage.hasNext()) {

      Document document = baseStorage.next();
      embeddingStoreIngestor.ingest(document);
      documentNumber ++;
    }
    JSONObject jsonObject = JsonUtils.createFolderIngestionStatusObject(storeName, documentNumber, documentParameters.getFileType());

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

    /**
   * Add document of type text, pdf and url to embedding store, provide the storeName (Index, Collection, etc).
     * @throws InterruptedException 
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-add-document-to-store")
  public InputStream addFileEmbedding(
                                        @Config Configuration configuration,
                                        @Alias("storeName") @DisplayName("Store Name") String storeName,
                                        @ParameterGroup(name = "Document")  DocumentParameters documentParameters,
                                        @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                                        @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_STORE_METADATA,configuration.getVectorStore());

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .dimension(embeddingModel.dimension())
        .build();

    EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

    EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChar(), segmentationParameters.getMaxOverlapSizeInChars()))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build();

    BaseStorage baseStorage = BaseStorage.builder()
        .configuration(configuration)
        .storageType(documentParameters.getStorageType())
        .contextPath(documentParameters.getContextPath())
        .fileType(documentParameters.getFileType())
        .build();
    Document document = baseStorage.getSingleDocument();

    embeddingStoreIngestor.ingest(document);

    JSONObject jsonObject = JsonUtils.createFileIngestionStatusObject(storeName, documentParameters.getFileType(), documentParameters.getContextPath());

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

  /**
   * Query information from embedding store , provide the storeName (Index, Collections, etc.)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-query-from-store")
  public InputStream queryFromEmbedding(
                                          @Config Configuration configuration,
                                          @Alias("storeName") @DisplayName("Store Name") String storeName,
                                          String question,
                                          Number maxResults,
                                          Double minScore,
                                          @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {
    int maximumResults = (int) maxResults;
    if (minScore == null) { //|| minScore == 0) {
      minScore = Constants.EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE;
    }

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .dimension(embeddingModel.dimension())
        .build();

    EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

    Embedding questionEmbedding = embeddingModel.embed(question).content();

    EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(questionEmbedding)
            .maxResults(maximumResults)
            .minScore(minScore)
            .build();

    EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
    List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();

    String information = embeddingMatches.stream()
        .map(match -> match.embedded().text())
        .collect(joining("\n\n"));

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", information);
    jsonObject.put("storeName", storeName);
    jsonObject.put("question", question);
    JSONArray sources = new JSONArray();

    JSONObject contentObject;
    for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
      Metadata matchMetadata = match.embedded().metadata();

      contentObject = new JSONObject();
      contentObject.put("embeddingId", match.embeddingId());
      contentObject.put("text", match.embedded().text());
      contentObject.put("score", match.score());

      JSONObject metadataObject = new JSONObject(matchMetadata.toMap());
      contentObject.put("metadata", metadataObject);

      sources.put(contentObject);
    }

    jsonObject.put("sources", sources);

    jsonObject.put("maxResults", maxResults);
    jsonObject.put("minScore", minScore);
    jsonObject.put("question", question);
    jsonObject.put("storeName", storeName);
    

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }



  /**
   * Query information from embedding store and filter results based on a metadata key filter
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-query-from-store-with-filter")
  public InputStream queryByFilterFromEmbedding(  String storeName,
                                                  String question,
                                                  Number maxResults,
                                                  Double minScore,
                                                  @Config Configuration configuration,
                                                  @ParameterGroup(name = "Filter") MetadataFilterParameters.SearchFilterParameters searchFilterParams,
                                                  @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA,configuration.getVectorStore());

    int maximumResults = (int) maxResults;
    if (minScore == null) { //|| minScore == 0) {
      minScore = Constants.EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE;
    }

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .dimension(embeddingModel.dimension())
        .build();

    EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

    Embedding questionEmbedding = embeddingModel.embed(question).content();

    EmbeddingSearchRequest.EmbeddingSearchRequestBuilder searchRequestBuilder = EmbeddingSearchRequest.builder()
            .queryEmbedding(questionEmbedding)
            .maxResults(maximumResults)
            .minScore(minScore);

    JSONObject jsonObject = new JSONObject();

    if(searchFilterParams.areFilterParamsSet()) {

      Filter filter = searchFilterParams.buildMetadataFilter();
      searchRequestBuilder.filter(filter);
      jsonObject.put("filter", searchFilterParams.getFilterJSONObject());
    }

    EmbeddingSearchRequest searchRequest = searchRequestBuilder.build();

    EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
    List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();

    String information = embeddingMatches.stream()
            .map(match -> match.embedded().text())
            .collect(joining("\n\n"));

    jsonObject.put("response", information);
    jsonObject.put("storeName", storeName);
    jsonObject.put("question", question);

    JSONArray sources = new JSONArray();

    JSONObject contentObject;
    String fullPath;
    for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
      Metadata matchMetadata = match.embedded().metadata();


      contentObject = new JSONObject();
      contentObject.put("embeddingId", match.embeddingId());
      contentObject.put("text", match.embedded().text());
      contentObject.put("score", match.score());

      JSONObject metadataObject = new JSONObject(matchMetadata.toMap());
      contentObject.put("metadata", metadataObject);

      sources.put(contentObject);
    }

    jsonObject.put("sources", sources);

    jsonObject.put("maxResults", maxResults);
    jsonObject.put("minScore", minScore);
    jsonObject.put("question", question);
    jsonObject.put("storeName", storeName);


    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }


  /**
   * Retrieves and lists sources from the specified embedding store.
   *
   * This method searches an embedding store for documents (sources) related to a simple query and collects metadata about
   * each matched document, such as file name, URL, and ingestion datetime. The results are returned as a JSON structure.
   *
   * @param storeName      the name of the embedding store to search
   * @param configuration  the configuration object providing access to connection details and other settings
   * @return an {@link InputStream} containing a JSON object with the store name and an array of source metadata.
   *
   * @MediaType(value = APPLICATION_JSON, strict = false)
   * @Alias("EMBEDDING-list-sources")
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-list-sources")
  public InputStream listSourcesFromStore(  String storeName,
                                            @Config Configuration configuration,
                                            @ParameterGroup(name = "Querying Strategy") QueryParameters queryParams
  ) {

    EmbeddingOperationValidator.validateOperationType(
        Constants.EMBEDDING_OPERATION_TYPE_QUERY_ALL,configuration.getVectorStore());
    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA,configuration.getVectorStore());

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .queryParams(queryParams)
        .build();

    JSONObject jsonObject = baseStore.listSources();

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }


  /**
   * Remove all documents based on a filter from a store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-remove-from-store-by-filter")
  public InputStream removeEmbeddingsByFilter(  String storeName,
                                                @Config Configuration configuration,
                                                @ParameterGroup(name = "Filter") MetadataFilterParameters.RemoveFilterParameters removeFilterParams,
                                                @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_REMOVE_EMBEDDINGS,configuration.getVectorStore());
    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA,configuration.getVectorStore());

    BaseModel baseModel = BaseModel.builder()
        .configuration(configuration)
        .embeddingModelParameters(embeddingModelParameters)
        .build();

    EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

    BaseStore baseStore = BaseStore.builder()
        .storeName(storeName)
        .configuration(configuration)
        .dimension(embeddingModel.dimension())
        .build();

    EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

    Filter filter = removeFilterParams.buildMetadataFilter();

    embeddingStore.removeAll(filter);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);
    jsonObject.put("filter", removeFilterParams.getFilterJSONObject());
    jsonObject.put("status", Constants.OPERATION_STATUS_DELETED);

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }
}
