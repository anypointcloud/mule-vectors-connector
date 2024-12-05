package org.mule.extension.vectors.internal.operation;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.DocumentResponseAttributes;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.config.DocumentConfiguration;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.DocumentErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.parameter.DocumentParameters;
import org.mule.extension.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.storage.BaseStorageConfiguration;
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
import java.util.stream.IntStream;

import static org.mule.extension.vectors.internal.helper.ResponseHelper.createDocumentResponse;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class DocumentOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentOperations.class);

  /**
   * Splits a document provided by full path in to a defined set of chucks and overlaps
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Document-split-into-chunks")
  @Throws(DocumentErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/DocumentSplitResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, DocumentResponseAttributes>
      documentSplitter( @Config DocumentConfiguration documentConfiguration,
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

      DocumentSplitter splitter = DocumentSplitters.recursive(
          segmentationParameters.getMaxSegmentSizeInChar(),
          segmentationParameters.getMaxOverlapSizeInChars());

      List<TextSegment> segments = splitter.split(document);

      // Use Streams to populate a JSONArray
      JSONArray jsonSegments = IntStream.range(0, segments.size())
          .mapToObj(i -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.JSON_KEY_TEXT, segments.get(i).text()); // Replace getText with the actual method
            jsonObject.put(Constants.JSON_KEY_INDEX, i);
            return jsonObject;
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(Constants.JSON_KEY_SEGMENTS, jsonSegments);

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
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Document-parser")
  @Throws(DocumentErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/DocumentParseResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, DocumentResponseAttributes>
      documentParser( @Config DocumentConfiguration documentConfiguration,
                      @Connection BaseStorageConnection storageConnection,
                      @ParameterGroup(name = "Document") DocumentParameters documentParameters
  ){

    try {

      BaseStorage baseStorage = BaseStorage.builder()
          .configuration(documentConfiguration)
          .connection(storageConnection)
          .contextPath(documentParameters.getContextPath())
          .fileType(documentParameters.getFileType())
          .build();
      Document document = baseStorage.getSingleDocument();

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(Constants.JSON_KEY_TEXT,document.text());

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
}
