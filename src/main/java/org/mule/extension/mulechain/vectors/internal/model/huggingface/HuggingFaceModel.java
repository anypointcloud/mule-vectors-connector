package org.mule.extension.mulechain.vectors.internal.model.huggingface;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.model.BaseModel;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class HuggingFaceModel  extends BaseModel {

  private final String apiKey;

  public HuggingFaceModel(Configuration configuration, EmbeddingModelNameParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);
    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    assert config != null;
    JSONObject modelConfig = config.getJSONObject("HUGGING_FACE");
    this.apiKey = modelConfig.getString("HUGGING_FACE_API_KEY");
  }

  public EmbeddingModel buildEmbeddingModel() {

    return HuggingFaceEmbeddingModel.builder()
        .accessToken(apiKey)
        .modelId(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
