package org.mule.extension.vectors.internal.operation;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.CompositeResponseAttributes;
import org.mule.extension.vectors.api.metadata.EmbeddingResponseAttributes;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.CompositeErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.EmbeddingOperationValidator;
import org.mule.extension.vectors.internal.helper.parameter.DocumentParameters;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.helper.parameter.MetadataFilterParameters;
import org.mule.extension.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.storage.BaseStorageConfiguration;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.extension.vectors.internal.util.JsonUtils;
import org.mule.extension.vectors.internal.util.MetadatatUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createCompositeResponse;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class CompositeOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompositeOperations.class);

  /**
   * Adds Text to Embedding Store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Composite-add-text-to-store")
  @DisplayName("[Composite] Add text to store")
  @Throws(CompositeErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreAddResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, CompositeResponseAttributes>
  addTextToStore( @Config CompositeConfiguration compositeConfiguration,
                  @Alias("text") @DisplayName("Text") @Content String text,
                  @Alias("storeName") @DisplayName("Store Name")  String storeName,
                  @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                  @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    try {

      LOGGER.debug(String.format("Adding text %s to store %s", text, storeName));

      BaseModel baseModel = BaseModel.builder()
          .connection(compositeConfiguration.getModelConfiguration().getConnection())
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .connection(compositeConfiguration.getStoreConfiguration().getConnection())
          .dimension(embeddingModel.dimension())
          .build();

      EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

      EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChars(), segmentationParameters.getMaxOverlapSizeInChars()))
          .embeddingModel(embeddingModel)
          .embeddingStore(embeddingStore)
          .build();

      Document document = new Document(text);
      MetadatatUtils.addMetadataToDocument(document, Constants.FILE_TYPE_TEXT);

      embeddingStoreIngestor.ingest(document);

      JSONObject jsonObject = JsonUtils.createIngestionStatusObject(storeName);

      return createCompositeResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while adding text \"%s\" into the store %s", text, storeName),
          MuleVectorsErrorType.COMPOSITE_OPERATIONS_FAILURE,
          e);
    }
  }


  /**
   * Loads multiple files from a folder into the embedding store. URLs are not supported with this operation.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Composite-add-folder-to-store")
  @DisplayName("[Composite] Add folder to store")
  @Throws(CompositeErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreAddResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, CompositeResponseAttributes>
  addFolderToStore( @Config CompositeConfiguration compositeConfiguration,
                    @Alias("storeName") @DisplayName("Store Name") String storeName,
                    @ConfigOverride @Alias("storage") @DisplayName(Constants.PARAM_DISPLAY_NAME_STORAGE_OVERRIDE)
                    BaseStorageConfiguration storageConfiguration,
                    @ParameterGroup(name = "Documents") DocumentParameters documentParameters,
                    @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                    @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    try {

      BaseStoreConnection storeConnection = compositeConfiguration.getStoreConfiguration().getConnection();

      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_STORE_METADATA, storeConnection.getVectorStore());

      BaseModel baseModel = BaseModel.builder()
          .connection(compositeConfiguration.getModelConfiguration().getConnection())
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .connection(storeConnection)
          .dimension(embeddingModel.dimension())
          .build();

      EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

      EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChars(), segmentationParameters.getMaxOverlapSizeInChars()))
          .embeddingModel(embeddingModel)
          .embeddingStore(embeddingStore)
          .build();

      BaseStorage baseStorage = BaseStorage.builder()
          .connection(storageConfiguration.getConnection())
          .contextPath(documentParameters.getContextPath())
          .fileType(documentParameters.getFileType())
          .build();

      long documentNumber = 0L;
      while(baseStorage.hasNext()) {

        try {

          Document document = baseStorage.next();
          MetadatatUtils.addIngestionMetadataToDocument(document);
          embeddingStoreIngestor.ingest(document);
          LOGGER.info(String.format("Ingested into %s  >> %s",
                                    storeName,
                                    MetadatatUtils.getSourceDisplayName(document.metadata())));
          documentNumber ++;
        } catch(Exception e) {
          // Do nothing continue to next iteration.
        }
      }
      JSONObject jsonObject = JsonUtils.createIngestionStatusObject(storeName);

      long finalDocumentNumber = documentNumber;
      return createCompositeResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
            put("fileType", documentParameters.getFileType());
            put("contextPath", documentParameters.getContextPath());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while adding folder %s into the store %s", documentParameters.getContextPath(), storeName),
          MuleVectorsErrorType.COMPOSITE_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Add document of type text, pdf and url to embedding store, provide the storeName (Index, Collection, etc).
   * @throws InterruptedException
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Composite-add-document-to-store")
  @DisplayName("[Composite] Add document to store")
  @Throws(CompositeErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreAddResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, CompositeResponseAttributes>
  addFileEmbedding( @Config CompositeConfiguration compositeConfiguration,
                    @Alias("storeName") @DisplayName("Store Name") String storeName,
                    @ConfigOverride @Alias("storage") @DisplayName(Constants.PARAM_DISPLAY_NAME_STORAGE_OVERRIDE)
                    BaseStorageConfiguration storageConfiguration,
                    @ParameterGroup(name = "Document")  DocumentParameters documentParameters,
                    @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                    @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {

      BaseStoreConnection storeConnection = compositeConfiguration.getStoreConfiguration().getConnection();

      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_STORE_METADATA, storeConnection.getVectorStore());

      BaseModel baseModel = BaseModel.builder()
          .connection(compositeConfiguration.getModelConfiguration().getConnection())
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .connection(storeConnection)
          .dimension(embeddingModel.dimension())
          .build();

      EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

      EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChars(), segmentationParameters.getMaxOverlapSizeInChars()))
          .embeddingModel(embeddingModel)
          .embeddingStore(embeddingStore)
          .build();

      BaseStorage baseStorage = BaseStorage.builder()
          .connection(storageConfiguration.getConnection())
          .contextPath(documentParameters.getContextPath())
          .fileType(documentParameters.getFileType())
          .build();
      Document document = baseStorage.getSingleDocument();

      MetadatatUtils.addIngestionMetadataToDocument(document);
      embeddingStoreIngestor.ingest(document);

      JSONObject jsonObject = JsonUtils.createIngestionStatusObject(storeName);

      return createCompositeResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
            put("fileType", documentParameters.getFileType());
            put("contextPath", documentParameters.getContextPath());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while adding document %s into the store %s", documentParameters.getContextPath(), storeName),
          MuleVectorsErrorType.COMPOSITE_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Query information from embedding store , provide the storeName (Index, Collections, etc.)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Composite-query-text-from-store")
  @DisplayName("[Composite] Query text from store")
  @Throws(CompositeErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreQueryResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, CompositeResponseAttributes>
  queryTextFromEmbedding( @Config CompositeConfiguration compositeConfiguration,
                          @Alias("storeName") @DisplayName("Store Name") String storeName,
                          String question,
                          Number maxResults,
                          Double minScore,
                          @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {
    try {

      BaseStoreConnection storeConnection = compositeConfiguration.getStoreConfiguration().getConnection();

      int maximumResults = maxResults.intValue();
      if (minScore == null) { //|| minScore == 0) {
        minScore = Constants.EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE;
      }

      BaseModel baseModel = BaseModel.builder()
          .connection(compositeConfiguration.getModelConfiguration().getConnection())
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .connection(storeConnection)
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
      jsonObject.put(Constants.JSON_KEY_RESPONSE, information);
      jsonObject.put(Constants.JSON_KEY_STORE_NAME, storeName);
      jsonObject.put(Constants.JSON_KEY_QUESTION, question);
      jsonObject.put(Constants.JSON_KEY_MAX_RESULTS, maxResults);
      jsonObject.put(Constants.JSON_KEY_MIN_SCORE, minScore);

      JSONArray sources = new JSONArray();

      JSONObject contentObject;
      for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
        Metadata matchMetadata = match.embedded().metadata();

        contentObject = new JSONObject();
        contentObject.put(Constants.JSON_KEY_EMBEDDING_ID, match.embeddingId());
        contentObject.put(Constants.JSON_KEY_TEXT, match.embedded().text());
        contentObject.put(Constants.JSON_KEY_SCORE, match.score());

        JSONObject metadataObject = new JSONObject(matchMetadata.toMap());
        contentObject.put(Constants.JSON_KEY_METADATA, metadataObject);

        sources.put(contentObject);
      }

      jsonObject.put(Constants.JSON_KEY_SOURCES, sources);

      return createCompositeResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while querying embeddings with question %s from the store %s", question, storeName),
          MuleVectorsErrorType.COMPOSITE_OPERATIONS_FAILURE,
          e);
    }
  }



  /**
   * Query information from embedding store and filter results based on a metadata key filter
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Composite-query-text-from-store-with-filter")
  @DisplayName("[Composite] Query text from store with filter")
  @Throws(CompositeErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreQueryResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, CompositeResponseAttributes>
  queryTextWithFilterFromEmbedding( String storeName,
                                    String question,
                                    Number maxResults,
                                    Double minScore,
                                    @Config CompositeConfiguration compositeConfiguration,
                                    @ParameterGroup(name = "Filter") MetadataFilterParameters.SearchFilterParameters searchFilterParams,
                                    @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {

      BaseStoreConnection storeConnection = compositeConfiguration.getStoreConfiguration().getConnection();

      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA, storeConnection.getVectorStore());

      int maximumResults = maxResults.intValue();
      if (minScore == null) { //|| minScore == 0) {
        minScore = Constants.EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE;
      }

      BaseModel baseModel = BaseModel.builder()
          .connection(compositeConfiguration.getModelConfiguration().getConnection())
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .connection(storeConnection)
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
        //jsonObject.put("filter", searchFilterParams.getFilterJSONObject());
      }

      EmbeddingSearchRequest searchRequest = searchRequestBuilder.build();

      EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
      List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();

      String information = embeddingMatches.stream()
          .map(match -> match.embedded().text())
          .collect(joining("\n\n"));

      jsonObject.put(Constants.JSON_KEY_RESPONSE, information);
      jsonObject.put(Constants.JSON_KEY_STORE_NAME, storeName);
      jsonObject.put(Constants.JSON_KEY_QUESTION, question);
      jsonObject.put(Constants.JSON_KEY_MAX_RESULTS, maxResults);
      jsonObject.put(Constants.JSON_KEY_MIN_SCORE, minScore);

      JSONArray sources = new JSONArray();

      JSONObject contentObject;
      for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
        Metadata matchMetadata = match.embedded().metadata();


        contentObject = new JSONObject();
        contentObject.put(Constants.JSON_KEY_EMBEDDING_ID, match.embeddingId());
        contentObject.put(Constants.JSON_KEY_TEXT, match.embedded().text());
        contentObject.put(Constants.JSON_KEY_SCORE, match.score());

        JSONObject metadataObject = new JSONObject(matchMetadata.toMap());
        contentObject.put(Constants.JSON_KEY_METADATA, metadataObject);

        sources.put(contentObject);
      }

      jsonObject.put(Constants.JSON_KEY_SOURCES, sources);


      return createCompositeResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
            put("filter", searchFilterParams.getFilterJSONObject());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while querying embeddings with question %s from the store %s", question, storeName),
          MuleVectorsErrorType.COMPOSITE_OPERATIONS_FAILURE,
          e);
    }
  }
}
