package org.mule.extension.vectors.internal.model.mistralai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

import static org.mule.extension.vectors.internal.util.JsonUtils.readConfigFile;

public class MistralAIModel  extends BaseModel {

  private final String apiKey;

  public MistralAIModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);

    MistralAIModelConfiguration mistralAIModelConfiguration = (MistralAIModelConfiguration) configuration.getModelConfiguration();
    this.apiKey = mistralAIModelConfiguration.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return MistralAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
