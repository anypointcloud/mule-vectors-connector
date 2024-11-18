package org.mule.extension.mulechain.vectors.internal.operation;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.data.segment.TextSegment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.api.metadata.DocumentResponseAttributes;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.mulechain.vectors.internal.error.provider.DocumentErrorTypeProvider;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.DocumentParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.FileTypeParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.extension.mulechain.vectors.internal.storage.BaseStorage;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static org.mule.extension.mulechain.vectors.internal.helper.ResponseHelper.createDocumentResponse;
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
      documentSplitter(@Config Configuration configuration,
                       @ParameterGroup(name = "Document") DocumentParameters documentParameters,
                       @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters){

    try {

      BaseStorage baseStorage = BaseStorage.builder()
          .configuration(configuration)
          .storageType(documentParameters.getStorageType())
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
            jsonObject.put("text", segments.get(i).text()); // Replace getText with the actual method
            jsonObject.put("index", i);
            return jsonObject;
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("segments", jsonSegments);

      return createDocumentResponse(jsonObject.toString(), new HashMap<>());

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
      documentParser( @Config Configuration configuration,
                      @ParameterGroup(name = "Document") DocumentParameters documentParameters){

    try {

      BaseStorage baseStorage = BaseStorage.builder()
          .configuration(configuration)
          .storageType(documentParameters.getStorageType())
          .contextPath(documentParameters.getContextPath())
          .fileType(documentParameters.getFileType())
          .build();
      Document document = baseStorage.getSingleDocument();

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("text",document.text());

      return createDocumentResponse(jsonObject.toString(), new HashMap<>());

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while splitting document %s.", documentParameters.getContextPath()),
          MuleVectorsErrorType.DOCUMENT_OPERATIONS_FAILURE,
          e);
    }
  }
}
