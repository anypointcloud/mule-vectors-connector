package org.mule.extension.vectors.internal.operation;

import dev.langchain4j.data.document.Document;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.DocumentResponseAttributes;
import org.mule.extension.vectors.internal.config.DocumentConfiguration;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.DocumentErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.parameter.DocumentParameters;
import org.mule.extension.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.pagination.DocumentPagingProvider;
import org.mule.extension.vectors.internal.metadata.DocumentsOutputTypeMetadataResolver;
import org.mule.extension.vectors.internal.util.JsonUtils;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createDocumentResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class DocumentOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentOperations.class);

  /**
   * Parses a document by filepath and returns the text
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Document-load-single")
  @DisplayName("[Document] Load single document")
  @Throws(DocumentErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/DocumentLoadSingleResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, DocumentResponseAttributes>
  loadSingleDocument( @Config DocumentConfiguration documentConfiguration,
                @Connection BaseStorageConnection storageConnection,
                @ParameterGroup(name = "Document") DocumentParameters documentParameters,
                @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters
  ){

    try {

      BaseStorage baseStorage = BaseStorage.builder()
          .configuration(documentConfiguration)
          .connection(storageConnection)
          .contextPath(documentParameters.getContextPath())
          .fileType(documentParameters.getFileType())
          .build();
      Document document = baseStorage.getSingleDocument();

      JSONObject jsonObject = JsonUtils.docToTextSegmentsJson(document,
                                                              segmentationParameters.getMaxSegmentSizeInChars(),
                                                              segmentationParameters.getMaxOverlapSizeInChars());

      return createDocumentResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("fileType", documentParameters.getFileType());
            put("contextPath", documentParameters.getContextPath());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while splitting document %s.", documentParameters.getContextPath()),
          MuleVectorsErrorType.DOCUMENT_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Parses a document by filepath and returns the text
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Document-load-list")
  @DisplayName("[Document] Load document list")
  @Throws(DocumentErrorTypeProvider.class)
  @OutputResolver(output = DocumentsOutputTypeMetadataResolver.class)
  public PagingProvider<BaseStorageConnection, Result<CursorProvider, DocumentResponseAttributes>>
  loadDocumentList( @Config DocumentConfiguration documentConfiguration,
                 @ParameterGroup(name = "Document") DocumentParameters documentParameters,
                 @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                 StreamingHelper streamingHelper
  ){

    try {
      return new DocumentPagingProvider(documentConfiguration,
                                        documentParameters,
                                        segmentationParameters,
                                        streamingHelper);

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while splitting document %s.", documentParameters.getContextPath()),
          MuleVectorsErrorType.DOCUMENT_OPERATIONS_FAILURE,
          e);
    }
  }
}
