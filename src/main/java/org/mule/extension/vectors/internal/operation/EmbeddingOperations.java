package org.mule.extension.vectors.internal.operation;

import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
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
  @DisplayName("[Embedding] Generate embeddings from text")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/EmbeddingGenerateFromTextResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, EmbeddingResponseAttributes>
      generateEmbedding(@Config EmbeddingConfiguration embeddingConfiguration,
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
      JSONArray jsonSegments = IntStream.range(0, embeddings.size())
          .mapToObj(i -> {
            JSONObject jsonSegment = new JSONObject();
            jsonSegment.put(Constants.JSON_KEY_TEXT, textSegments.get(i).text());
            jsonSegment.put(Constants.JSON_KEY_EMBEDDING, Arrays.toString(embeddings.get(i).vector())); // Replace getText with the actual method
            jsonSegment.put(Constants.JSON_KEY_INDEX, i);
            return jsonSegment;
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      jsonObject.put(Constants.JSON_KEY_SEGMENTS, jsonSegments);
      jsonObject.put(Constants.JSON_KEY_DIMENSIONS, embeddingModel.dimension());

      return createEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("embeddingModelName", embeddingModelParameters.getEmbeddingModelName());
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
}
