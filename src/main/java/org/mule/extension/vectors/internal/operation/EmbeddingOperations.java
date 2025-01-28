package org.mule.extension.vectors.internal.operation;

import static org.mule.extension.vectors.internal.constant.Constants.JSON_KEY_BASE64DATA;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createEmbeddingResponse;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createMultimodalEmbeddingResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.EmbeddingResponseAttributes;
import org.mule.extension.vectors.api.metadata.MultimodalEmbeddingResponseAttributes;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.error.provider.EmbeddingErrorTypeProvider;
import org.mule.extension.vectors.internal.helper.media.ImageProcessor;
import org.mule.extension.vectors.internal.helper.parameter.ImageProcessorParameters;
import org.mule.extension.vectors.internal.helper.parameter.MediaProcessorParameters;
import org.mule.extension.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.extension.vectors.internal.model.multimodal.EmbeddingMultimodalModel;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.*;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for embedding operations, providing methods to generate embeddings from text or documents.
 */
public class EmbeddingOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingOperations.class);

  /**
   * Generates embeddings from a given text string. The text can optionally be segmented before embedding.
   *
   * @param embeddingConfiguration the configuration for the embedding service.
   * @param modelConnection the connection to the embedding model.
   * @param text the input text to generate embeddings from.
   * @param segmentationParameters parameters defining segmentation rules for the input text.
   * @param embeddingModelParameters parameters for the embedding model to be used.
   * @return a {@link org.mule.runtime.extension.api.runtime.operation.Result} containing the embeddings in JSON format and metadata.
   * @throws ModuleException if an error occurs during the embedding process.
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
                            @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {
      BaseModel baseModel = BaseModel.builder()
          .configuration(embeddingConfiguration)
          .connection(modelConnection)
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      EmbeddingModel embeddingModel = baseModel.buildEmbeddingModel();

      LOGGER.debug(String.format("Embedding model for %s service built.", modelConnection.getEmbeddingModelService()));

      List<TextSegment> textSegments;
      if(segmentationParameters.getMaxSegmentSizeInChars() > 0) {

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(segmentationParameters.getMaxSegmentSizeInChars(),
                                                                        segmentationParameters.getMaxOverlapSizeInChars());
        textSegments = documentSplitter.split(new Document(text));
      } else {

        textSegments = new LinkedList<>();
        textSegments.add(TextSegment.from(text));
      }
      List<Embedding> embeddings;
      try {

        embeddings = embeddingModel.embedAll(textSegments).content();

      }  catch(ModuleException e) {

        throw e;

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
   * Generates embeddings from text segments provided in a document format.
   *
   * @param embeddingConfiguration the configuration for the embedding service.
   * @param modelConnection the connection to the embedding model.
   * @param content the input text segments as an {@link InputStream} in JSON format.
   * @param embeddingModelParameters parameters for the embedding model to be used.
   * @return a {@link org.mule.runtime.extension.api.runtime.operation.Result} containing the embeddings in JSON format and metadata.
   * @throws ModuleException if an error occurs during the embedding process.
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
                                @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

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

      } catch(ModuleException e) {

        throw e;

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

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-generate-from-image-and-text")
  @DisplayName("[Embedding] Generate from image and text")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/EmbeddingGenerateResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, MultimodalEmbeddingResponseAttributes>
  generateEmbeddingFromImageAndText(@Config EmbeddingConfiguration embeddingConfiguration,
                                    @Connection BaseModelConnection modelConnection,
                                    @Alias("image") @DisplayName("Image Binary Content") @Content InputStream imageInputStream,
                                    @Alias("text") @DisplayName("Text") @Summary("Short text describing the image") @Content String text,
                                    @Alias("mediaProcessorParameters") @ParameterGroup(name="Image Processor Settings")
                                        @DisplayName("Processor Settings") @Summary("Image Processor Settings") @Expression(ExpressionSupport.NOT_SUPPORTED) ImageProcessorParameters imageProcessorParameters,
                                    @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {
      // Convert InputStream to byte array
      byte[] imageBytes = IOUtils.toByteArray(imageInputStream);

      ImageProcessor imageProcessor = ImageProcessor.builder()
          .targetWidth(imageProcessorParameters.getTargetWidth())
          .targetHeight(imageProcessorParameters.getTargetHeight())
          .compressionQuality(imageProcessorParameters.getCompressionQuality())
          .scaleStrategy(imageProcessorParameters.getScaleStrategy())
          .build();

      imageBytes =  imageProcessor.process(imageBytes);

      JSONObject jsonObject = new JSONObject();

      List<TextSegment> textSegments = new LinkedList<>();
      textSegments.add(TextSegment.from(text));
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

      BaseModel baseModel = BaseModel.builder()
          .configuration(embeddingConfiguration)
          .connection(modelConnection)
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      // Assuming you have a multimodal embedding model method
      EmbeddingMultimodalModel multimodalEmbeddingModel = (EmbeddingMultimodalModel) baseModel.buildEmbeddingMultimodalModel();

      Embedding embedding = multimodalEmbeddingModel.embedTextAndImage(text, imageBytes).content();

      JSONArray jsonEmbeddings = new JSONArray();
      jsonEmbeddings.put(embedding.vector());

      jsonObject.put(Constants.JSON_KEY_EMBEDDINGS, jsonEmbeddings);

      jsonObject.put(Constants.JSON_KEY_DIMENSION, multimodalEmbeddingModel.dimension());

      return createMultimodalEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("embeddingModelName", embeddingModelParameters.getEmbeddingModelName());
            put("embeddingModelDimension", multimodalEmbeddingModel.dimension());
          }}
      );

    } catch (Exception e) {
      throw new ModuleException(
          "Error while generating embedding from image and text",
          MuleVectorsErrorType.EMBEDDING_OPERATIONS_FAILURE,
          e);
    }
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Embedding-generate-from-media-and-text")
  @DisplayName("[Embedding] Generate from media and text")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/metadata/EmbeddingGenerateResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, MultimodalEmbeddingResponseAttributes>
  generateEmbeddingFromMediaAndText(@Config EmbeddingConfiguration embeddingConfiguration,
                                    @Connection BaseModelConnection modelConnection,
                                    @Alias("media") @DisplayName("Media") @InputJsonType(schema = "api/metadata/MediaLoadSingleResponse.json") @Content InputStream mediaContent,
                                    @Alias("text") @DisplayName("Text") @Summary("Short text describing the image") @Content String text,
                                    @ParameterGroup(name = "Embedding Model") EmbeddingModelParameters embeddingModelParameters) {

    try {

      String mediaContentString = IOUtils.toString(mediaContent, StandardCharsets.UTF_8);
      JSONObject jsonMediaObject = new JSONObject(mediaContentString);

      JSONObject jsonObject = new JSONObject();

      List<TextSegment> textSegments = new LinkedList<>();
      textSegments.add(TextSegment.from(text));
      JSONArray jsonTextSegments = IntStream.range(0, textSegments.size())
          .mapToObj(i -> {
            JSONObject jsonSegment = new JSONObject();
            jsonSegment.put(Constants.JSON_KEY_TEXT, textSegments.get(i).text());
            JSONObject jsonMetadata = jsonMediaObject.getJSONObject(Constants.JSON_KEY_METADATA);
            jsonMetadata.put(Constants.JSON_KEY_INDEX, i);
            jsonSegment.put(Constants.JSON_KEY_METADATA, jsonMetadata);
            return jsonSegment;
          })
          .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

      jsonObject.put(Constants.JSON_KEY_TEXT_SEGMENTS, jsonTextSegments);

      BaseModel baseModel = BaseModel.builder()
          .configuration(embeddingConfiguration)
          .connection(modelConnection)
          .embeddingModelParameters(embeddingModelParameters)
          .build();

      // Assuming you have a multimodal embedding model method
      EmbeddingMultimodalModel multimodalEmbeddingModel = (EmbeddingMultimodalModel) baseModel.buildEmbeddingMultimodalModel();

      Embedding embedding = multimodalEmbeddingModel.embedTextAndImage(
                                                                          text,
                                                                          Base64.getDecoder().decode(jsonMediaObject.getString(JSON_KEY_BASE64DATA))
                                                                      ).content();

      JSONArray jsonEmbeddings = new JSONArray();
      jsonEmbeddings.put(embedding.vector());

      jsonObject.put(Constants.JSON_KEY_EMBEDDINGS, jsonEmbeddings);

      jsonObject.put(Constants.JSON_KEY_DIMENSION, multimodalEmbeddingModel.dimension());

      return createMultimodalEmbeddingResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("embeddingModelName", embeddingModelParameters.getEmbeddingModelName());
            put("embeddingModelDimension", multimodalEmbeddingModel.dimension());
          }}
      );

    } catch (Exception e) {
      throw new ModuleException(
          "Error while generating embedding from image and text",
          MuleVectorsErrorType.EMBEDDING_OPERATIONS_FAILURE,
          e);
    }
  }
}
