package org.mule.extension.mulechain.vectors.internal.model.azureopenai;

import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.mulechain.vectors.internal.model.BaseModel;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class AzureOpenAIModel extends BaseModel {

  private final String apiKey;
  private final String endpoint;

  public AzureOpenAIModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);
    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    assert config != null;
    JSONObject modelConfig = config.getJSONObject(Constants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI);
    this.apiKey = modelConfig.getString("AZURE_OPENAI_KEY");
    this.endpoint = modelConfig.getString("AZURE_OPENAI_ENDPOINT");
  }

  public EmbeddingModel buildEmbeddingModel() {

    return AzureOpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .endpoint(endpoint)
        .deploymentName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
