package org.mule.extension.mulechain.vectors.internal.operation;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;

import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.EmbeddingOperationValidator;
import org.mule.extension.mulechain.vectors.internal.helper.EmbeddingStoreIngestorHelper;
import org.mule.extension.mulechain.vectors.internal.helper.factory.EmbeddingModelFactory;
import org.mule.extension.mulechain.vectors.internal.helper.factory.EmbeddingStoreFactory;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.FileTypeParameters;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.MetadataFilterParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import static java.util.stream.Collectors.joining;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.StorageTypeParameters;

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
  public InputStream addTextToStore(String storeName, String textToAdd, @Config Configuration configuration, @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams){

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    TextSegment textSegment = TextSegment.from(textToAdd);
    Embedding textEmbedding = embeddingModel.embed(textSegment).content();
    store.add(textEmbedding, textSegment); 
    
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("status", "added");
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
  public InputStream generateEmbedding(String textToAdd, @Config Configuration configuration, @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams){

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    TextSegment textSegment = TextSegment.from(textToAdd);
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
  public InputStream addFolderToStore(String storeName, String folderPath, @Config Configuration configuration,
                                @ParameterGroup(name = "Context") FileTypeParameters fileType,
                                @ParameterGroup(name = "Storage") StorageTypeParameters storageType,
                                int maxSegmentSizeInChars, int maxOverlapSizeInChars,
                                @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams){

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_STORE_METADATA,configuration.getVectorStore());

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars))
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();

    EmbeddingStoreIngestorHelper embeddingStoreIngestorHelper = new EmbeddingStoreIngestorHelper(ingestor, storeName);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject jsonObject = new JSONObject();
    System.out.println("Storage Type: " + storageType.getStorageType());
    if (storageType.getStorageType().equals("S3") && !fileType.getFileType().equals("url")) {
      JSONObject s3Json = config.getJSONObject("S3");
      String awsKey = s3Json.getString("AWS_ACCESS_KEY_ID");
      String awsSecret = s3Json.getString("AWS_SECRET_ACCESS_KEY");
      String awsRegion = s3Json.getString("AWS_DEFAULT_REGION");
      String s3Bucket = s3Json.getString("AWS_S3_BUCKET");
      jsonObject = embeddingStoreIngestorHelper.ingestFromS3Folder(folderPath, fileType, awsKey, awsSecret, awsRegion, s3Bucket);
    } else {
      jsonObject = embeddingStoreIngestorHelper.ingestFromLocalFolder(folderPath, fileType);
    }

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

    /**
   * Add document of type text, pdf and url to embedding store, provide the storeName (Index, Collection, etc).
     * @throws InterruptedException 
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-add-document-to-store")
  public InputStream addFileEmbedding(String storeName, String contextPath, @Config Configuration configuration,
                                 @ParameterGroup(name = "Context") FileTypeParameters fileType,
                                 @ParameterGroup(name = "Storage") StorageTypeParameters storageType,
                                 int maxSegmentSizeInChars, int maxOverlapSizeInChars,
                                 @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_STORE_METADATA,configuration.getVectorStore());
                                  
    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars))
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();

    EmbeddingStoreIngestorHelper embeddingStoreIngestorHelper = new EmbeddingStoreIngestorHelper(ingestor, storeName);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject jsonObject = new JSONObject();
    System.out.println("Storage Type: " + storageType.getStorageType());
    if (storageType.getStorageType().equals("S3") && !fileType.getFileType().equals("url")) {
      JSONObject s3Json = config.getJSONObject("S3");
      String awsKey = s3Json.getString("AWS_ACCESS_KEY_ID");
      String awsSecret = s3Json.getString("AWS_SECRET_ACCESS_KEY");
      String awsRegion = s3Json.getString("AWS_DEFAULT_REGION");
      String s3Bucket = s3Json.getString("AWS_S3_BUCKET");
      jsonObject = embeddingStoreIngestorHelper.ingestFromS3File(contextPath, fileType, awsKey, awsSecret, awsRegion, s3Bucket);
    } else if (storageType.getStorageType().equals("AZURE_BLOB") && !fileType.getFileType().equals("url")) {
      JSONObject azJson = config.getJSONObject("AZURE_BLOB");
      String azureName = azJson.getString("AZURE_BLOB_ACCOUNT_NAME");
      String azureKey = azJson.getString("AZURE_BLOB_ACCOUNT_KEY");
      String[] parts = contextPath.split("/", 2);
      jsonObject = embeddingStoreIngestorHelper.ingestFromAZFile(parts[0], parts[1], fileType, azureName, azureKey);
    } else {
      jsonObject = embeddingStoreIngestorHelper.ingestFromLocalFile(contextPath, fileType);
    }

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

  /**
   * Query information from embedding store , provide the storeName (Index, Collections, etc.)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-query-from-store")
  public InputStream queryFromEmbedding(String storeName, String question, Number maxResults, Double minScore, 
                                  @Config Configuration configuration,
                                  @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams) {
    int maximumResults = (int) maxResults;
    if (minScore == null) { //|| minScore == 0) {
      minScore = Constants.EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE;
    }

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    Embedding questionEmbedding = embeddingModel.embed(question).content();

    EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(questionEmbedding)
            .maxResults(maximumResults)
            .minScore(minScore)
            .build();

    EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
    List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();

    String information = embeddingMatches.stream()
        .map(match -> match.embedded().text())
        .collect(joining("\n\n"));

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", information);
    jsonObject.put("storeName", storeName);
    jsonObject.put("question", question);
    JSONArray sources = new JSONArray();
    String absoluteDirectoryPath;
    String fileName;
    String url;
    String ingestionDateTime;

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
   * Query information from embedding store and filter results based on a metadata key filter
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-query-from-store-with-filter")
  public InputStream queryByFilterFromEmbedding(String storeName, String question, Number maxResults, Double minScore,
                                        @Config Configuration configuration,
                                        @ParameterGroup(name = "Filter") MetadataFilterParameters.SearchFilterParameters searchFilterParams,
                                        @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA,configuration.getVectorStore());

    int maximumResults = (int) maxResults;
    if (minScore == null) { //|| minScore == 0) {
      minScore = Constants.EMBEDDING_SEARCH_REQUEST_DEFAULT_MIN_SCORE;
    }

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

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

    EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
    List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();

    String information = embeddingMatches.stream()
            .map(match -> match.embedded().text())
            .collect(joining("\n\n"));

    jsonObject.put("response", information);
    jsonObject.put("storeName", storeName);
    jsonObject.put("question", question);

    JSONArray sources = new JSONArray();
    String absoluteDirectoryPath;
    String fileName;
    String url;
    String ingestionDateTime;

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
   * @param modelParams    the parameter group that specifies additional model properties
   * @return an {@link InputStream} containing a JSON object with the store name and an array of source metadata.
   *
   * @MediaType(value = APPLICATION_JSON, strict = false)
   * @Alias("EMBEDDING-list-sources")
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-list-sources")
  public InputStream listSourcesFromStore(String storeName,
                                        @Config Configuration configuration,
                                        @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA,configuration.getVectorStore());

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);
    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    Embedding queryEmbedding = embeddingModel.embed(".").content();
    EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(16384)
            .minScore(0.0)
            .build();

    EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
    List<EmbeddingMatch<TextSegment>> embeddingMatches = searchResult.matches();
    String information = embeddingMatches.stream()
            .map(match -> match.embedded().text())
            .collect(joining("\n\n"));

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);
    JSONArray sources = new JSONArray();
    String absoluteDirectoryPath;
    String fileName;
    String url;
    String ingestionDatetime;

    JSONObject contentObject;
    String fullPath;

    HashMap<String, JSONObject> sourcesJSONObjectHashMap = new  HashMap<String, JSONObject>();
    for (EmbeddingMatch<TextSegment> match : embeddingMatches) {

      Metadata matchMetadata = match.embedded().metadata();
      fileName = matchMetadata.getString(Constants.METADATA_KEY_FILE_NAME);
      url = matchMetadata.getString(Constants.METADATA_KEY_URL);
      fullPath = matchMetadata.getString(Constants.METADATA_KEY_FULL_PATH);
      absoluteDirectoryPath = matchMetadata.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH);
      ingestionDatetime = matchMetadata.getString(Constants.METADATA_KEY_INGESTION_DATETIME);

      contentObject = new JSONObject();
      contentObject.put(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
      contentObject.put(Constants.METADATA_KEY_FULL_PATH, fullPath);
      contentObject.put(Constants.METADATA_KEY_FILE_NAME, fileName);
      contentObject.put(Constants.METADATA_KEY_URL, url);
      contentObject.put(Constants.METADATA_KEY_INGESTION_DATETIME, ingestionDatetime);

      String key =
              ((fullPath != null &&!fullPath.isEmpty()) ? fullPath :
                      (url != null && !url.isEmpty()) ? url : "") +
              ((ingestionDatetime != null && !ingestionDatetime.isEmpty()) ? ingestionDatetime : "");

      // Add contentObject to sources only if it has at least one key-value pair
      if (!contentObject.isEmpty() && !key.isEmpty()) {

        sourcesJSONObjectHashMap.put(key, contentObject);
      }
    }

    jsonObject.put("sources", JsonUtils.jsonObjectCollectionToJsonArray(sourcesJSONObjectHashMap.values()));

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }


  /**
   * Remove all documents based on a filter from a store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-remove-documents-by-filter")
  public InputStream removeDocumentsByFilter(String storeName,
                                            @Config Configuration configuration,
                                             @ParameterGroup(name = "Filter") MetadataFilterParameters.RemoveFilterParameters removeFilterParams,
                                            @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams) {

    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_REMOVE_EMBEDDINGS,configuration.getVectorStore());
    EmbeddingOperationValidator.validateOperationType(
            Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA,configuration.getVectorStore());

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);
    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    Filter filter = removeFilterParams.buildMetadataFilter();

    store.removeAll(filter);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);
    jsonObject.put("filter", removeFilterParams.getFilterJSONObject());
    jsonObject.put("status", "deleted");

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }
}
