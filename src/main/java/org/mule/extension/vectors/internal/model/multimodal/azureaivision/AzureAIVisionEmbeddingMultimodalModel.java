package org.mule.extension.vectors.internal.model.multimodal.azureaivision;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.mule.extension.vectors.internal.model.multimodal.EmbeddingMultimodalModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AzureAIVisionEmbeddingMultimodalModel implements EmbeddingMultimodalModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureAIVisionEmbeddingMultimodalModel.class);

  private final String modelName;
  private final Integer maxRetries;
  
  public AzureAIVisionEmbeddingMultimodalModel(String modelName,
                                               Integer maxRetries) {

    this.modelName = modelName != null ? modelName : AzureAIVisionEmbeddingMultimodalModelName.getDefaultModelName();
    this.maxRetries = maxRetries != null ? maxRetries : 3;
  }

  @Override
  public Integer dimension() {
    return 0;
  }

  @Override
  public Response<Embedding> embedText(String text) {
    return null;
  }

  @Override
  public Response<Embedding> embedImage(byte[] imageBytes) {
    return null;
  }

  @Override
  public Response<Embedding> embedTextAndImage(String text, byte[] imageBytes) {
    return null;
  }

  @Override
  public Response<List<Embedding>> embedTexts(List<String> texts) {
    return null;
  }

  @Override
  public Response<List<Embedding>> embedImages(List<byte[]> imageBytesList) {
    return null;
  }

  public static class Builder {

    private String modelName;
    private Integer maxRetries;

    public Builder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public Builder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public AzureAIVisionEmbeddingMultimodalModel build() {

      return new AzureAIVisionEmbeddingMultimodalModel(modelName, maxRetries);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
