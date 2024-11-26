package org.mule.extension.vectors.internal.model.openai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

import static org.mule.extension.vectors.internal.util.JsonUtils.readConfigFile;

public class OpenAIModel  extends BaseModel {

  private final String apiKey;

  public OpenAIModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);

    OpenAIModelConfiguration openAIModelConfiguration = (OpenAIModelConfiguration) configuration.getModelConfiguration();
    this.apiKey = openAIModelConfiguration.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return OpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
