package org.mule.extension.vectors.internal.model.vertexai;

import com.google.auth.oauth2.ServiceAccountCredentials;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.vertexai.VertexAIModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.error.MuleVectorsErrorType;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.io.ByteArrayInputStream;

// https://docs.langchain4j.dev/integrations/language-models/google-vertex-ai-gemini/
public class VertexAIModel extends BaseModel {

  private static final String LOCATION = "us-central1";
  private static final String ENDPOINT = "us-central1-aiplatform.googleapis.com:443";
  private static final String PUBLISHER = "google";

  private final String projectId;
  private final String clientEmail;
  private final String clientId;
  private final String privateKeyId;
  private final String privateKey;

  public VertexAIModel(EmbeddingConfiguration embeddingConfiguration, VertexAIModelConnection vertexAiModelConnection, EmbeddingModelParameters embeddingModelParameters) {

    super(embeddingConfiguration, vertexAiModelConnection, embeddingModelParameters);
    this.projectId = vertexAiModelConnection.getProjectId();
    this.clientEmail = vertexAiModelConnection.getClientEmail();
    this.clientId = vertexAiModelConnection.getClientId();
    this.privateKeyId = vertexAiModelConnection.getPrivateKeyId();
    this.privateKey = vertexAiModelConnection.getPrivateKey();
  }

  private String buildJsonCredentials() {
    return new StringBuilder()
        .append("{")
        .append("\"type\": \"service_account\",")
        .append("\"project_id\": \"").append(this.projectId).append("\",")
        .append("\"private_key_id\": \"").append(this.privateKeyId).append("\",")
        .append("\"private_key\": \"").append(this.privateKey).append("\",")
        .append("\"client_email\": \"").append(this.clientEmail).append("\",")
        .append("\"client_id\": \"").append(this.clientId).append("\",")
        .append("\"auth_uri\": \"").append(Constants.GCP_AUTH_URI).append("\",")
        .append("\"token_uri\": \"").append(Constants.GCP_TOKEN_URI).append("\",")
        .append("\"auth_provider_x509_cert_url\": \"").append(Constants.GCP_AUTH_PROVIDER_X509_CERT_URL).append("\",")
        .append("\"client_x509_cert_url\": \"").append(Constants.GCP_CLIENT_X509_CERT_URL).append(this.clientEmail).append("\",")
        .append("\"universe_domain\": \"googleapis.com\"")
        .append("}")
        .toString();
  }

  public EmbeddingModel buildEmbeddingModel() {

    try {

      ServiceAccountCredentials
          serviceAccountCredentials = ServiceAccountCredentials.fromStream(new ByteArrayInputStream(buildJsonCredentials().getBytes()));

      return VertexAiEmbeddingModel.builder()
          .project(projectId)
          .credentials(serviceAccountCredentials)
          .location(LOCATION)
          .endpoint(ENDPOINT)
          .publisher(PUBLISHER)
          .modelName(embeddingModelParameters.getEmbeddingModelName())
          .build();

    } catch (Exception e) {

      throw new ModuleException(
          String.format("Error initializing Vertex AI Embedding Model."),
          MuleVectorsErrorType.STORAGE_SERVICES_FAILURE,
          e);
    }
  }
}
