package org.mule.extension.vectors.internal.model.huggingface;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

public class HuggingFaceModel  extends BaseModel {

  private final String apiKey;

  public HuggingFaceModel(CompositeConfiguration compositeConfiguration, EmbeddingModelParameters embeddingModelParameters) {

    super(compositeConfiguration, embeddingModelParameters);

    HuggingFaceModelConfiguration huggingFaceModelConfiguration =(HuggingFaceModelConfiguration) compositeConfiguration.getModelConfiguration();
    this.apiKey = huggingFaceModelConfiguration.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return HuggingFaceEmbeddingModel.builder()
        .accessToken(apiKey)
        .modelId(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
