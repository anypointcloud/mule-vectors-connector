package org.mule.extension.mulechain.vectors.internal.operation;

import com.fasterxml.jackson.databind.JsonNode;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.mule.extension.mulechain.vectors.internal.constants.Constants;
import org.mule.extension.mulechain.vectors.internal.helpers.EmbeddingModelFactory;
import org.mule.extension.mulechain.vectors.internal.helpers.EmbeddingStoreFactory;
import org.mule.extension.mulechain.vectors.internal.helpers.parameters.FileTypeParameters;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.helpers.parameters.MetadataFilterParameters;
import org.mule.extension.mulechain.vectors.internal.helpers.parameters.EmbeddingModelNameParameters;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import static java.util.stream.Collectors.joining;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.Result;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.extension.mulechain.vectors.internal.storage.S3FileReader;
import org.mule.extension.mulechain.vectors.internal.helpers.parameters.StorageTypeParameters;

import org.mule.extension.mulechain.vectors.internal.storage.AzureFileReader;
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

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars))
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject jsonObject = new JSONObject();
    System.out.println("Storage Type: " + storageType.getStorageType());
    if (storageType.getStorageType().equals("S3") && !fileType.getFileType().equals("url")) {
      JSONObject s3Json = config.getJSONObject("S3");
      String awsKey = s3Json.getString("AWS_ACCESS_KEY_ID");
      String awsSecret = s3Json.getString("AWS_SECRET_ACCESS_KEY");
      String awsRegion = s3Json.getString("AWS_DEFAULT_REGION");
      String s3Bucket = s3Json.getString("AWS_S3_BUCKET");
      jsonObject = ingestFromS3Folder(folderPath, ingestor, storeName, fileType, awsKey, awsSecret, awsRegion, s3Bucket);
    } else {
      jsonObject = ingestFromLocalFolder(folderPath, ingestor, storeName, fileType);
    }

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }
    
  private JSONObject ingestFromLocalFolder(String folderPath, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType) {
    long totalFiles = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      totalFiles = paths.filter(Files::isRegularFile).count();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Total number of files to process: " + totalFiles);
    AtomicInteger fileCounter = new AtomicInteger(0);
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      paths.filter(Files::isRegularFile).forEach(file -> {
        int currentFileCounter = fileCounter.incrementAndGet();
        System.out.println("Processing file " + currentFileCounter + ": " + file.getFileName());
        Document document = null;
        switch (fileType.getFileType()) {
          case Constants.FILE_TYPE_CRAWL:
            document = loadDocument(file.toString(), new TextDocumentParser());
            addMetadata(Paths.get(file.toString()), document);
            ingestor.ingest(document);    
            break;
          case Constants.FILE_TYPE_TEXT:
            document = loadDocument(file.toString(), new TextDocumentParser());
            System.out.println("File: " + file.toString());
            document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
            document.metadata().add(Constants.METADATA_KEY_FILE_NAME, file.getFileName());
            document.metadata().add(Constants.METADATA_KEY_FULL_PATH, folderPath + file.getFileName());
            ingestor.ingest(document);
            break;
          case Constants.FILE_TYPE_ANY:
            document = loadDocument(file.toString(), new ApacheTikaDocumentParser());
            System.out.println("File: " + file.toString());
            document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_ANY);
            document.metadata().add(Constants.METADATA_KEY_FILE_NAME, file.getFileName());
            document.metadata().add(Constants.METADATA_KEY_FULL_PATH, folderPath + file.getFileName());
            ingestor.ingest(document);
            break;
          default:
            throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Processing complete ");
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("filesCount", totalFiles);
    jsonObject.put("folderPath", folderPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  private JSONObject ingestFromS3Folder(String folderPath, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String awsKey, String awsSecret, String awsRegion, String s3Bucket)
  {
        S3FileReader s3FileReader = new S3FileReader(s3Bucket, awsKey, awsSecret, awsRegion);
        long totalFiles = s3FileReader.readAllFiles(folderPath, ingestor, fileType);   
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("filesCount", totalFiles);
        jsonObject.put("folderPath", folderPath);
        jsonObject.put("storeName", storeName);
        jsonObject.put("status", "updated");
        return jsonObject; 
  }

  private JSONObject ingestFromS3File(String folderPath, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String awsKey, String awsSecret, String awsRegion, String s3Bucket)
  {
        S3FileReader s3FileReader = new S3FileReader(s3Bucket, awsKey, awsSecret, awsRegion);
        s3FileReader.readFile(folderPath, fileType, ingestor);   
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileType", fileType.getFileType());
        jsonObject.put("folderPath", folderPath);
        jsonObject.put("storeName", storeName);
        jsonObject.put("status", "updated");
        return jsonObject; 
  }

  private JSONObject ingestFromAZContainer(String containerName, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String azureName, String azureKey)
  {
        AzureFileReader azFileReader = new AzureFileReader(azureName, azureKey);
        azFileReader.readAllFiles(containerName, ingestor, fileType);   
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileType", fileType.getFileType());
        jsonObject.put("folderPath", containerName);
        jsonObject.put("storeName", storeName);
        jsonObject.put("status", "updated");
        return jsonObject; 
  }
  private JSONObject ingestFromAZFile(String containerName, String blobName, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String azureName, String azureKey)
  {
        AzureFileReader azFileReader = new AzureFileReader(azureName, azureKey);
        azFileReader.readFile(containerName, blobName, fileType, ingestor);   
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileType", fileType.getFileType());
        jsonObject.put("folderPath", containerName);
        jsonObject.put("storeName", storeName);
        jsonObject.put("status", "updated");
        return jsonObject; 
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

                                  
    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);

    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars))
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject jsonObject = new JSONObject();
    System.out.println("Storage Type: " + storageType.getStorageType());
    if (storageType.getStorageType().equals("S3") && !fileType.getFileType().equals("url")) {
      JSONObject s3Json = config.getJSONObject("S3");
      String awsKey = s3Json.getString("AWS_ACCESS_KEY_ID");
      String awsSecret = s3Json.getString("AWS_SECRET_ACCESS_KEY");
      String awsRegion = s3Json.getString("AWS_DEFAULT_REGION");
      String s3Bucket = s3Json.getString("AWS_S3_BUCKET");
      jsonObject = ingestFromS3File(contextPath, ingestor, storeName, fileType, awsKey, awsSecret, awsRegion, s3Bucket);
    } else if (storageType.getStorageType().equals("AZURE_BLOB") && !fileType.getFileType().equals("url")) {
      JSONObject azJson = config.getJSONObject("AZURE_BLOB");
      String azureName = azJson.getString("AZURE_BLOB_ACCOUNT_NAME");
      String azureKey = azJson.getString("AZURE_BLOB_ACCOUNT_KEY");
      String[] parts = contextPath.split("/", 2);
      jsonObject = ingestFromAZFile(parts[0], parts[1], ingestor, storeName, fileType, azureName, azureKey);
    } else {
      jsonObject = ingestFromLocalFile(contextPath, ingestor, storeName, fileType);
    }

    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }

  private void addMetadata(Path filePath, Document document) {
    try {
      String fileContent = new String(Files.readAllBytes(filePath));
      JsonNode jsonNode = JsonUtils.stringToJsonNode(fileContent.toString());
      String content = jsonNode.path("content").asText();
      String source_url = jsonNode.path("url").asText();
      String title = jsonNode.path("title").asText();
      document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
      document.metadata().add(Constants.METADATA_KEY_FILE_NAME, title);
      document.metadata().add(Constants.METADATA_KEY_FULL_PATH, source_url);
      document.metadata().put("source", source_url);
      document.metadata().add("title", title);
    } catch (IOException e) { 
      System.err.println("Error accessing folder: " + e.getMessage());
    }

  }

  private JSONObject ingestFromLocalFile(String contextPath, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType) {

    System.out.println("file Type: " + fileType.getFileType());
    
    Document document = null;
    Path filePath; 
    String fileName;

    switch (fileType.getFileType()) {
      case Constants.FILE_TYPE_CRAWL:
        filePath = Paths.get(contextPath.toString()); 
        fileName = getFileNameFromPath(contextPath);

        document = loadDocument(filePath.toString(), new TextDocumentParser());
        addMetadata(filePath, document);
        ingestor.ingest(document);

        break;
      case Constants.FILE_TYPE_TEXT:
        filePath = Paths.get(contextPath.toString()); 
        fileName = getFileNameFromPath(contextPath);
        document = loadDocument(filePath.toString(), new TextDocumentParser());
        document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
        document.metadata().add(Constants.METADATA_KEY_FILE_NAME, fileName);
        document.metadata().add(Constants.METADATA_KEY_FULL_PATH, contextPath);
        ingestor.ingest(document);


        break;
      case Constants.FILE_TYPE_ANY:
        filePath = Paths.get(contextPath.toString()); 
        fileName = getFileNameFromPath(contextPath);
        document = loadDocument(filePath.toString(), new ApacheTikaDocumentParser());
        document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_ANY);
        document.metadata().add(Constants.METADATA_KEY_FILE_NAME, fileName);
        document.metadata().add(Constants.METADATA_KEY_FULL_PATH, contextPath);
        ingestor.ingest(document);

        break;
      case Constants.FILE_TYPE_URL:
        System.out.println("Context Path: " + contextPath);

        URL url = null;
        try {
          url = new URL(contextPath);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }

        Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
        HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
        document = transformer.transform(htmlDocument);
        document.metadata().add(Constants.METADATA_KEY_URL, contextPath);
        ingestor.ingest(document);

        break;
      default:
        throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
    }



    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType.getFileType());
    jsonObject.put("filePath", contextPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");

    return jsonObject;
  }


  private String getFileNameFromPath(String fullPath) {

      File file = new File(fullPath);
      return file.getName();
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
      minScore = 0.7;
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
    String textSegment;

    JSONObject contentObject;
    String fullPath;
    for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
      Metadata matchMetadata = match.embedded().metadata();

      fileName = matchMetadata.getString(Constants.METADATA_KEY_FILE_NAME);
      url = matchMetadata.getString(Constants.METADATA_KEY_URL);
      fullPath = matchMetadata.getString(Constants.METADATA_KEY_FULL_PATH);
      absoluteDirectoryPath = matchMetadata.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH);
      textSegment = matchMetadata.getString("textSegment");

      contentObject = new JSONObject();
      contentObject.put("absoluteDirectoryPath", absoluteDirectoryPath);
      contentObject.put("full_path", fullPath);
      contentObject.put("file_name", fileName);
      contentObject.put("url", url);
      contentObject.put("individualScore", match.score());
      
      contentObject.put("textSegment", match.embedded().text());
      sources.put(contentObject);
    }

    jsonObject.put("sources", sources);

    jsonObject.put("maxResults", maxResults);
    jsonObject.put("minimumScore", minScore);
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
    int maximumResults = (int) maxResults;
    if (minScore == null) { //|| minScore == 0) {
      minScore = 0.7;
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
    String textSegment;

    JSONObject contentObject;
    String fullPath;
    for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
      Metadata matchMetadata = match.embedded().metadata();

      fileName = matchMetadata.getString(Constants.METADATA_KEY_FILE_NAME);
      url = matchMetadata.getString(Constants.METADATA_KEY_URL);
      fullPath = matchMetadata.getString(Constants.METADATA_KEY_FULL_PATH);
      absoluteDirectoryPath = matchMetadata.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH);
      textSegment = matchMetadata.getString("textSegment");

      contentObject = new JSONObject();
      contentObject.put("absoluteDirectoryPath", absoluteDirectoryPath);
      contentObject.put("full_path", fullPath);
      contentObject.put("file_name", fileName);
      contentObject.put("url", url);
      contentObject.put("individualScore", match.score());

      contentObject.put("textSegment", match.embedded().text());
      sources.put(contentObject);
    }

    jsonObject.put("sources", sources);

    jsonObject.put("maxResults", maxResults);
    jsonObject.put("minimumScore", minScore);
    jsonObject.put("question", question);
    jsonObject.put("storeName", storeName);


    return toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
  }


  interface AssistantSources {

    Result<String> chat(String userMessage);
  }

  /**
   * List all documents from a store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-list-documents")
  public InputStream listDocumentsFromStore(String storeName,
                                        @Config Configuration configuration,
                                        @ParameterGroup(name = "Additional Properties") EmbeddingModelNameParameters modelParams) {

    EmbeddingModel embeddingModel = EmbeddingModelFactory.createModel(configuration, modelParams);
    EmbeddingStore<TextSegment> store = EmbeddingStoreFactory.createStore(configuration, storeName, embeddingModel.dimension());

    Embedding queryEmbedding = embeddingModel.embed(".").content();
    EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(Integer.MAX_VALUE)
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

    JSONObject contentObject;
    String fullPath;
    for (EmbeddingMatch<TextSegment> match : embeddingMatches) {
      Metadata matchMetadata = match.embedded().metadata();
      fileName = matchMetadata.getString(Constants.METADATA_KEY_FILE_NAME);
      url = matchMetadata.getString(Constants.METADATA_KEY_URL);
      fullPath = matchMetadata.getString(Constants.METADATA_KEY_FULL_PATH);
      absoluteDirectoryPath = matchMetadata.getString(Constants.METADATA_KEY_ABSOLUTE_DIRECTORY_PATH);

      contentObject = new JSONObject();
      contentObject.put("absoluteDirectoryPath", absoluteDirectoryPath);
      contentObject.put("full_path", fullPath);
      contentObject.put("file_name", fileName);
      contentObject.put("url", url);

      // Add contentObject to sources only if it has at least one key-value pair
      if (!contentObject.isEmpty()) {
        sources.put(contentObject);
      }
    }

    jsonObject.put("documents", sources);

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
