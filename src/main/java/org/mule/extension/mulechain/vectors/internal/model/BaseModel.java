package org.mule.extension.mulechain.vectors.internal.model;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.mulechain.vectors.internal.model.azureopenai.AzureOpenAIModel;
import org.mule.extension.mulechain.vectors.internal.model.huggingface.HuggingFaceModel;
import org.mule.extension.mulechain.vectors.internal.model.mistralai.MistralAIModel;
import org.mule.extension.mulechain.vectors.internal.model.nomic.NomicModel;
import org.mule.extension.mulechain.vectors.internal.model.openai.OpenAIModel;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseModel.class);

  protected Configuration configuration;
  protected EmbeddingModelParameters embeddingModelParameters;

  public BaseModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    this.configuration = configuration;
    this.embeddingModelParameters = embeddingModelParameters;
  }

  public EmbeddingModel buildEmbeddingModel() {

    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  public static BaseModel.Builder builder() {

    return new BaseModel.Builder();
  }

  public static class Builder {

    private Configuration configuration;
    private EmbeddingModelParameters embeddingModelParameters;

    public Builder() {

    }

    public BaseModel.Builder configuration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public BaseModel.Builder embeddingModelParameters(EmbeddingModelParameters embeddingModelParameters) {
      this.embeddingModelParameters = embeddingModelParameters;
      return this;
    }

    public BaseModel build() {

      BaseModel baseModel;

      LOGGER.debug("Embedding Model Service: " + configuration.getEmbeddingModelService());
      switch (configuration.getEmbeddingModelService()) {

        case Constants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI:
          baseModel = new AzureOpenAIModel(configuration, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_OPENAI:
          baseModel = new OpenAIModel(configuration, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_MISTRAL_AI:
          baseModel = new MistralAIModel(configuration, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_NOMIC:
          baseModel = new NomicModel(configuration, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE:
          baseModel = new HuggingFaceModel(configuration, embeddingModelParameters);
          break;

        default:
          throw new ModuleException(
              String.format("Error while initializing embedding model service. \"%s\" is not supported.", configuration.getEmbeddingModelService()),
              MuleVectorsErrorType.AI_SERVICES_FAILURE);
      }
      return baseModel;
    }
  }
}
