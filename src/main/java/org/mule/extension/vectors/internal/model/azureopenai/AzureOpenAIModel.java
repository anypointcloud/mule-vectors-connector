package org.mule.extension.vectors.internal.model.azureopenai;

import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.model.azureopenai.AzureOpenAIModelConnection;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;

public class AzureOpenAIModel extends BaseModel {

  private final String endpoint;
  private final String apiKey;

  public AzureOpenAIModel(EmbeddingConfiguration embeddingConfiguration,
                          AzureOpenAIModelConnection azureOpenAIModelConnection,
                          EmbeddingModelParameters embeddingModelParameters) {

    super(embeddingConfiguration, azureOpenAIModelConnection, embeddingModelParameters);

    this.endpoint = azureOpenAIModelConnection.getEndpoint();
    this.apiKey = azureOpenAIModelConnection.getApiKey();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return AzureOpenAiEmbeddingModel.builder()
        .apiKey(apiKey)
        .endpoint(endpoint)
        .deploymentName(embeddingModelParameters.getEmbeddingModelName())
        .build();
  }
}
