package org.mule.extension.vectors.internal.model;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.model.azureopenai.AzureOpenAIModelConnection;
import org.mule.extension.vectors.internal.connection.model.einstein.EinsteinModelConnection;
import org.mule.extension.vectors.internal.connection.model.huggingface.HuggingFaceModelConnection;
import org.mule.extension.vectors.internal.connection.model.mistralai.MistralAIModelConnection;
import org.mule.extension.vectors.internal.connection.model.nomic.NomicModelConnection;
import org.mule.extension.vectors.internal.connection.model.openai.OpenAIModelConnection;
import org.mule.extension.vectors.internal.connection.model.vertexai.VertexAIModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.azureopenai.AzureOpenAIModel;
import org.mule.extension.vectors.internal.model.einstein.EinsteinModel;
import org.mule.extension.vectors.internal.model.huggingface.HuggingFaceModel;
import org.mule.extension.vectors.internal.model.mistralai.MistralAIModel;
import org.mule.extension.vectors.internal.model.nomic.NomicModel;
import org.mule.extension.vectors.internal.model.openai.OpenAIModel;
import org.mule.extension.vectors.internal.model.vertexai.VertexAIModel;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseModel.class);

  protected EmbeddingConfiguration embeddingConfiguration;
  protected BaseModelConnection modelConnection;
  protected EmbeddingModelParameters embeddingModelParameters;

  public BaseModel(EmbeddingConfiguration embeddingConfiguration, BaseModelConnection modelConnection, EmbeddingModelParameters embeddingModelParameters) {

    this.embeddingConfiguration = embeddingConfiguration;
    this.modelConnection = modelConnection;
    this.embeddingModelParameters = embeddingModelParameters;
  }

  public EmbeddingModel buildEmbeddingModel() {

    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  public static BaseModel.Builder builder() {

    return new BaseModel.Builder();
  }

  public static class Builder {

    private EmbeddingConfiguration embeddingConfiguration;
    protected BaseModelConnection modelConnection;
    private EmbeddingModelParameters embeddingModelParameters;

    public Builder() {

    }

    public BaseModel.Builder configuration(EmbeddingConfiguration embeddingConfiguration) {
      this.embeddingConfiguration = embeddingConfiguration;
      return this;
    }

    public BaseModel.Builder connection(BaseModelConnection modelConnection) {
      this.modelConnection = modelConnection;
      return this;
    }

    public BaseModel.Builder embeddingModelParameters(EmbeddingModelParameters embeddingModelParameters) {
      this.embeddingModelParameters = embeddingModelParameters;
      return this;
    }

    public BaseModel build() {

      BaseModel baseModel;

      LOGGER.debug("Embedding Model Service: " + modelConnection.getEmbeddingModelService());
      switch (modelConnection.getEmbeddingModelService()) {

        case Constants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI:
          baseModel = new AzureOpenAIModel(embeddingConfiguration, (AzureOpenAIModelConnection) modelConnection, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_OPENAI:
          baseModel = new OpenAIModel(embeddingConfiguration, (OpenAIModelConnection) modelConnection, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_MISTRAL_AI:
          baseModel = new MistralAIModel(embeddingConfiguration, (MistralAIModelConnection) modelConnection, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_NOMIC:
          baseModel = new NomicModel(embeddingConfiguration, (NomicModelConnection) modelConnection, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE:
          baseModel = new HuggingFaceModel(embeddingConfiguration, (HuggingFaceModelConnection) modelConnection, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_EINSTEIN:
          baseModel = new EinsteinModel(embeddingConfiguration, (EinsteinModelConnection) modelConnection, embeddingModelParameters);
          break;

        case Constants.EMBEDDING_MODEL_SERVICE_VERTEX_AI:
          baseModel = new VertexAIModel(embeddingConfiguration, (VertexAIModelConnection) modelConnection, embeddingModelParameters);
          break;

        default:
          throw new ModuleException(
              String.format("Error while initializing embedding model service. \"%s\" is not supported.", modelConnection.getEmbeddingModelService()),
              MuleVectorsErrorType.AI_SERVICES_FAILURE);
      }
      return baseModel;
    }
  }
}
