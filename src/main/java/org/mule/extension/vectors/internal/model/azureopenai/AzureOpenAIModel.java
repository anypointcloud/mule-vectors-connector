package org.mule.extension.vectors.internal.model.azureopenai;

import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

import static org.mule.extension.vectors.internal.util.JsonUtils.readConfigFile;

public class AzureOpenAIModel extends BaseModel {

  private final String endpoint;
  private final String apiKey;

  public AzureOpenAIModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);

    AzureOpenAIModelConfiguration azureOpenAIModelConfiguration = (AzureOpenAIModelConfiguration) configuration.getModelConfiguration();
    this.endpoint = azureOpenAIModelConfiguration.getEndpoint();
    this.apiKey = azureOpenAIModelConfiguration.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return AzureOpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .endpoint(endpoint)
        .deploymentName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
