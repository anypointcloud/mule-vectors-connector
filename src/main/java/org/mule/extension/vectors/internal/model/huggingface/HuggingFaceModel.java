package org.mule.extension.vectors.internal.model.huggingface;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

import static org.mule.extension.vectors.internal.util.JsonUtils.readConfigFile;

public class HuggingFaceModel  extends BaseModel {

  private final String apiKey;

  public HuggingFaceModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);

    HuggingFaceModelConfiguration huggingFaceModelConfiguration =(HuggingFaceModelConfiguration) configuration.getModelConfiguration();
    this.apiKey = huggingFaceModelConfiguration.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return HuggingFaceEmbeddingModel.builder()
        .accessToken(apiKey)
        .modelId(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
