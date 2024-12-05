package org.mule.extension.vectors.internal.operation;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.EmbeddingResponseAttributes;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.CompositeErrorTypeProvider;
import org.mule.extension.vectors.internal.error.provider.StoreErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.EmbeddingOperationValidator;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.helper.parameter.MetadataFilterParameters;
import org.mule.extension.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.extension.vectors.internal.store.BaseStore;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;

import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class StoreOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreOperations.class);

  /**
   * Retrieves and lists sources from the specified embedding store.
   *
   * This method searches an embedding store for documents (sources) related to a simple query and collects metadata about
   * each matched document, such as file name, URL, and ingestion datetime. The results are returned as a JSON structure.
   *
   * @param storeName      the name of the embedding store to search
   * @param compositeConfiguration  the configuration object providing access to connection details and other settings
   * @return an {@link InputStream} containing a JSON object with the store name and an array of source metadata.
   *
   * @MediaType(value = APPLICATION_JSON, strict = false)
   * @Alias("Store-list-sources")
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Store-list-sources")
  @DisplayName("[Store] List sources")
  @Throws(StoreErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StoreListSourcesResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
  listSourcesFromStore( String storeName,
                        @Config CompositeConfiguration compositeConfiguration,
                        @ParameterGroup(name = "Querying Strategy") QueryParameters queryParams
  ) {

    try {

      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_QUERY_ALL, compositeConfiguration.getStoreConfiguration().getVectorStore());
      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA, compositeConfiguration.getStoreConfiguration().getVectorStore());

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .configuration(compositeConfiguration)
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
  @OutputJsonType(schema = "api/response/StoreRemoveFromStoreResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
  removeEmbeddingsByFilter( String storeName,
                            @Config CompositeConfiguration compositeConfiguration,
                            @ParameterGroup(name = "Filter") MetadataFilterParameters.RemoveFilterParameters removeFilterParams,
                            @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {
      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_REMOVE_EMBEDDINGS, compositeConfiguration.getStoreConfiguration().getVectorStore());
      EmbeddingOperationValidator.validateOperationType(
          Constants.EMBEDDING_OPERATION_TYPE_FILTER_BY_METADATA, compositeConfiguration.getStoreConfiguration().getVectorStore());

      BaseModel baseModel = BaseModel.builder()
          .connection(compositeConfiguration.getModelConfiguration().getConnection())
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      BaseStore baseStore = BaseStore.builder()
          .storeName(storeName)
          .configuration(compositeConfiguration)
          .dimension(embeddingModel.dimension())
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
