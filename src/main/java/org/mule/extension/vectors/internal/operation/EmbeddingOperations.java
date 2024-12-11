package org.mule.extension.vectors.internal.operation;

import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import org.apache.commons.io.IOUtils;
import org.mule.extension.vectors.api.metadata.EmbeddingResponseAttributes;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.EmbeddingErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.parameter.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;

import static java.util.stream.Collectors.joining;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class EmbeddingOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingOperations.class);

   /**
   * Generate Embeddings from text
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-generate-from-text")
  @DisplayName("[Embedding] Generate from text")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/EmbeddingGenerateResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
      generateEmbeddingFromText(@Config EmbeddingConfiguration embeddingConfiguration,
                        @Connection BaseModelConnection modelConnection,
                        @Alias("text") @DisplayName("Text") @Content String text,
                        @ParameterGroup(name = "Segmentation") SegmentationParameters segmentationParameters,
                        @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    try {

      BaseModel baseModel = BaseModel.builder()
          .configuration(embeddingConfiguration)
          .connection(modelConnection)
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      DocumentSplitter documentSplitter = DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChars(),
                                                                      segmentationParameters.getMaxOverlapSizeInChars());

      List<TextSegment> textSegments = documentSplitter.split(new Document(text));
      List<Embedding> embeddings;
      try {

        embeddings = embeddingModel.embedAll(textSegments).content();

      } catch(Exception e) {

        throw new ModuleException(
            String.format("Error while generating embedding from text \"%s\"", text),
            MuleVectorsErrorType.AI_SERVICES_FAILURE,
            e);
      }

      JSONObject jsonObject = new JSONObject();

      JSONArray jsonTextSegments = IntStream.range(0, textSegments.size())
          .mapToObj(i -> {
            JSONObject jsonSegment = new JSONObject();
            jsonSegment.put(Constants.JSON_KEY_TEXT, textSegments.get(i).text());
            JSONObject jsonMetadata = new JSONObject();
            jsonMetadata.put(Constants.JSON_KEY_INDEX, i);
            jsonSegment.put(Constants.JSON_KEY_METADATA, jsonMetadata);
            return jsonSegment;
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      jsonObject.put(Constants.JSON_KEY_TEXT_SEGMENTS, jsonTextSegments);

      JSONArray jsonEmbeddings = IntStream.range(0, embeddings.size())
          .mapToObj(i -> {
            return embeddings.get(i).vector();
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      jsonObject.put(Constants.JSON_KEY_EMBEDDINGS, jsonEmbeddings);

      jsonObject.put(Constants.JSON_KEY_DIMENSION, embeddingModel.dimension());

      return createEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("embeddingModelName", embeddingModelParameters.getEmbeddingModelName());
            put("embeddingModelDimension", embeddingModel.dimension());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while generating embedding from text \"%s\"", text),
          MuleVectorsErrorType.EMBEDDING_OPERATIONS_FAILURE,
          e);
    }
  }

  /**
   * Generate Embeddings from text
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-generate-from-document")
  @DisplayName("[Embedding] Generate from document")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/EmbeddingGenerateResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
  generateEmbeddingFromDocument(@Config EmbeddingConfiguration embeddingConfiguration,
                                @Connection BaseModelConnection modelConnection,
                                @Alias("textSegments") @DisplayName("Text Segments") @InputJsonType(schema = "api/metadata/DocumentLoadSingleResponse.json") @Content InputStream content,
                                @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters){

    try {

      String contentString = IOUtils.toString(content, StandardCharsets.UTF_8);

      BaseModel baseModel = BaseModel.builder()
          .configuration(embeddingConfiguration)
          .connection(modelConnection)
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      JSONObject jsonObject = new JSONObject(contentString);
      JSONArray jsonTextSegments = jsonObject.getJSONArray(Constants.JSON_KEY_TEXT_SEGMENTS);

      List<TextSegment> textSegments = new LinkedList<>();

      IntStream.range(0, jsonTextSegments.length())
          .mapToObj(jsonTextSegments::getJSONObject) // Convert index to JSONObject
          .forEach(jsonTextSegment -> {
            Metadata metadata = Metadata.from(jsonTextSegment.getJSONObject(Constants.JSON_KEY_METADATA).toMap());
            textSegments.add(new TextSegment(jsonTextSegment.getString(Constants.JSON_KEY_TEXT), metadata));
          });

      List<Embedding> embeddings;
      try {

        embeddings = embeddingModel.embedAll(textSegments).content();

      } catch(Exception e) {

        throw new ModuleException(
            String.format("Error while generating embedding from document \"%s\"", ""),
            MuleVectorsErrorType.AI_SERVICES_FAILURE,
            e);
      }

      JSONArray jsonEmbeddings = IntStream.range(0, embeddings.size())
          .mapToObj(i -> {
            return embeddings.get(i).vector();
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      jsonObject.put(Constants.JSON_KEY_EMBEDDINGS, jsonEmbeddings);

      jsonObject.put(Constants.JSON_KEY_DIMENSION, embeddingModel.dimension());

      return createEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("embeddingModelName", embeddingModelParameters.getEmbeddingModelName());
            put("embeddingModelDimension", embeddingModel.dimension());
          }});

    } catch (ModuleException me) {
      throw me;

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error while generating embedding from document \"%s\"", ""),
          MuleVectorsErrorType.EMBEDDING_OPERATIONS_FAILURE,
          e);
    }
  }
}
