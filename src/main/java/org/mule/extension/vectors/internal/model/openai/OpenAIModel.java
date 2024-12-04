package org.mule.extension.vectors.internal.model.openai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

public class OpenAIModel  extends BaseModel {

  private final String apiKey;

  public OpenAIModel(CompositeConfiguration compositeConfiguration, EmbeddingModelParameters embeddingModelParameters) {

    super(compositeConfiguration, embeddingModelParameters);

    OpenAIModelConfiguration openAIModelConfiguration = (OpenAIModelConfiguration) compositeConfiguration.getModelConfiguration();
    this.apiKey = openAIModelConfiguration.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return OpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
