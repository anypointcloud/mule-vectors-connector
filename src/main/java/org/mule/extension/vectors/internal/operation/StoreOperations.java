package org.mule.extension.vectors.internal.operation;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.EmbeddingResponseAttributes;
import org.mule.extension.vectors.api.metadata.StoreResponseAttributes;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.config.StoreConfiguration;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.StoreErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.EmbeddingOperationValidator;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.helper.parameter.MetadataFilterParameters;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.extension.vectors.internal.util.JsonUtils;
import org.mule.extension.vectors.internal.util.MetadatatUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createStoreResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class StoreOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreOperations.class);

  /**
   * Generate Embeddings from text
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Store-add")
  @DisplayName("[Store] Add")
  @Throws(StoreErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreAddResponse.json")
  public Result<InputStream, StoreResponseAttributes>
      addToStore( @Config StoreConfiguration storeConfiguration,
                  @Connection BaseStoreConnection storeConnection,
                  String storeName,
                  @Alias("textSegmentsAndEmbeddings") @DisplayName("Text Segments and Embeddings") @InputJsonType(schema = "api/metadata/EmbeddingGenerateResponse.json") @Content InputStream content){

    try {

      String contentString = IOUtils.toString(content, StandardCharsets.UTF_8);

      JSONObject jsonContent = new JSONObject(contentString);

      HashMap<String, Object> ingestionMetadataMap = MetadatatUtils.getIngestionMetadata();

      JSONArray jsonTextSegments = jsonContent.getJSONArray(Constants.JSON_KEY_TEXT_SEGMENTS);
      List<TextSegment> textSegments = new LinkedList<>();
      IntStream.range(0, jsonTextSegments.length())
          .mapToObj(jsonTextSegments::getJSONObject) // Convert index to JSONObject
          .forEach(jsonTextSegment -> {
            HashMap<String, Object> metadataMap = (HashMap<String, Object>)jsonTextSegment.getJSONObject(Constants.JSON_KEY_METADATA).toMap();
            metadataMap.putAll(ingestionMetadataMap);
            Metadata metadata = Metadata.from(metadataMap);
            textSegments.add(new TextSegment(jsonTextSegment.getString(Constants.JSON_KEY_TEXT), metadata));
          });

      JSONArray jsonEmbeddings = jsonContent.getJSONArray(Constants.JSON_KEY_EMBEDDINGS);
      List<Embedding> embeddings = new LinkedList<>();
      IntStream.range(0, jsonEmbeddings.length())
          .mapToObj(jsonEmbeddings::getJSONArray) // Convert index to JSONObject
          .forEach(jsonEmbedding -> {

            // Convert JSONArray to float[]
            float[] floatArray = new float[jsonEmbedding.length()];
            for (int i = 0; i < jsonEmbedding.length(); i++) {
              floatArray[i] = (float) jsonEmbedding.getDouble(i);
            }
            embeddings.add(new Embedding(floatArray));
          });

      int dimension = jsonContent.getInt(Constants.JSON_KEY_DIMENSION);
      ValidationUtils.ensureGreaterThanZero(dimension, Constants.JSON_KEY_DIMENSION);

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .configuration(storeConfiguration)
          .connection(storeConnection)
          .dimension(dimension)
          .build();

      EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

      try {
        embeddingStore.addAll(embeddings, textSegments);
        LOGGER.info(String.format("Ingested into %s  >> %s",
                                  storeName,
                                  MetadatatUtils.getSourceDisplayName(textSegments.get(0).metadata())));

      } catch(Exception e) {

        throw new ModuleException(
            String.format("Error while adding data to store \"%s\"", storeName),
            MuleVectorsErrorType.STORE_SERVICES_FAILURE,
            e);
      }

      JSONObject jsonObject = JsonUtils.createIngestionStatusObject(storeName);

      return createStoreResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while adding data to store \"%s\"", storeName),
          MuleVectorsErrorType.STORE_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Retrieves and lists sources from the specified embedding store.
   *
   * This method searches an embedding store for documents (sources) related to a simple query and collects metadata about
   * each matched document, such as file name, URL, and ingestion datetime. The results are returned as a JSON structure.
   *
   * @param storeName      the name of the embedding store to search
   * @param storeConfiguration  the configuration object providing access to connection details and other settings
   * @return an {@link InputStream} containing a JSON object with the store name and an array of source metadata.
   *
   * @MediaType(value = APPLICATION_JSON, strict = false)
   * @Alias("Store-list-sources")
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Store-list-sources")
  @DisplayName("[Store] List sources")
  @Throws(StoreErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreListSourcesResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
  listSourcesFromStore(  @Config StoreConfiguration storeConfiguration,
                         @Connection BaseStoreConnection storeConnection,
                        String storeName,
                        @ParameterGroup(name = "Querying Strategy") QueryParameters queryParams
  ) {

    try {

      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_QUERY_ALL, storeConnection.getVectorStore());
      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA, storeConnection.getVectorStore());

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .configuration(storeConfiguration)
          .connection(storeConnection)
          .queryParams(queryParams)
          .build();

      JSONObject jsonObject = baseStore.listSources();

      return createEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while listing sources from the store %s", storeName),
          MuleVectorsErrorType.STORE_OPERATIONS_FAILURE,
          e);
    }
  }


  /**
   * Remove all documents based on a filter from a store
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Store-remove-from-store-by-filter")
  @DisplayName("[Store] Remove from store by filter")
  @Throws(StoreErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/StoreRemoveFromStoreResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
  removeEmbeddingsByFilter( @Config StoreConfiguration storeConfiguration,
                            @Connection BaseStoreConnection storeConnection,
                            String storeName,
                            @ParameterGroup(name = "Filter") MetadataFilterParameters.RemoveFilterParameters removeFilterParams,
                            @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {
      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_REMOVE_EMBEDDINGS, storeConnection.getVectorStore());
      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA, storeConnection.getVectorStore());

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .configuration(storeConfiguration)
          .connection(storeConnection)
          .build();

      EmbeddingStore<TextSegment> embeddingStore = baseStore.buildEmbeddingStore();

      Filter filter = removeFilterParams.buildMetadataFilter();

      embeddingStore.removeAll(filter);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(Constants.JSON_KEY_STATUS, Constants.OPERATION_STATUS_DELETED);

      return createEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("storeName", storeName);
            put("filter", removeFilterParams.getFilterJSONObject());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {
      throw new ModuleException(
          String.format("Error while removing embeddings from the store %s", storeName),
          MuleVectorsErrorType.STORE_OPERATIONS_FAILURE,
          e);
    }
  }
}
