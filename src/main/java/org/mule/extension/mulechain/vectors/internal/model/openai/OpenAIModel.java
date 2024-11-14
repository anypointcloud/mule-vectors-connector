package org.mule.extension.mulechain.vectors.internal.model.openai;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.model.BaseModel;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class OpenAIModel  extends BaseModel {

  private final String apiKey;

  public OpenAIModel(Configuration configuration, EmbeddingModelNameParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);
    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    assert config != null;
    JSONObject modelConfig = config.getJSONObject("OPENAI");
    this.apiKey = modelConfig.getString("OPENAI_API_KEY");
  }

  public EmbeddingModel buildEmbeddingModel() {

    return OpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
