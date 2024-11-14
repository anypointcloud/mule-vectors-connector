package org.mule.extension.mulechain.vectors.internal.model.mistralai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.model.BaseModel;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class MistralAIModel  extends BaseModel {

  private final String apiKey;

  public MistralAIModel(Configuration configuration, EmbeddingModelNameParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);
    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    assert config != null;
    JSONObject modelConfig = config.getJSONObject("MISTRAL_AI");
    this.apiKey = modelConfig.getString("MISTRAL_AI_API_KEY");
  }

  public EmbeddingModel buildEmbeddingModel() {

    return MistralAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
