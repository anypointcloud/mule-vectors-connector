package org.mule.extension.vectors.internal.model.azureopenai;

import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

public class AzureOpenAIModel extends BaseModel {

  private final String endpoint;
  private final String apiKey;

  public AzureOpenAIModel(CompositeConfiguration compositeConfiguration, EmbeddingModelParameters embeddingModelParameters) {

    super(compositeConfiguration, embeddingModelParameters);

    AzureOpenAIModelConfiguration azureOpenAIModelConfiguration = (AzureOpenAIModelConfiguration) compositeConfiguration.getModelConfiguration();
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
