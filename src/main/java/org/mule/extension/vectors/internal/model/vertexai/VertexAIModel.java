package org.mule.extension.vectors.internal.model.vertexai;

import com.google.auth.Credentials;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.vertexai.VertexAIModelConnection;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.runtime.extension.api.exception.ModuleException;

// https://docs.langchain4j.dev/integrations/language-models/google-vertex-ai-gemini/
public class VertexAIModel extends BaseModel {

  private static final String DEFAULT_LOCATION = "us-central1";
  private static final String PUBLISHER = "google";

  private final String projectId;
  private final String location;
  private final Credentials credentials;

  public VertexAIModel(EmbeddingConfiguration embeddingConfiguration, VertexAIModelConnection vertexAiModelConnection, EmbeddingModelParameters embeddingModelParameters) {

    super(embeddingConfiguration, vertexAiModelConnection, embeddingModelParameters);

    try{

      this.projectId = vertexAiModelConnection.getProjectId();
      this.location = vertexAiModelConnection.getLocation();
      this.credentials = vertexAiModelConnection.getCredentials();
    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error initializing Vertex AI Model."),
          MuleVectorsErrorType.AI_SERVICES_FAILURE,
          e);
    }
  }

  public EmbeddingModel buildEmbeddingModel() {

    try {

      return VertexAiEmbeddingModel.builder()
          .project(projectId)
          .credentials(credentials)
          .location(location != null ? location : DEFAULT_LOCATION)
          .publisher(PUBLISHER)
          .modelName(embeddingModelParameters.getEmbeddingModelName())
          .build();

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error initializing Vertex AI Embedding Model."),
          MuleVectorsErrorType.AI_SERVICES_FAILURE,
          e);
    }
  }
}
