package org.mule.extension.vectors.internal.model.einstein;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.CompositeConfiguration;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.einstein.EinsteinModelConnection;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EinsteinModel  extends BaseModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinModel.class);

  private final String salesforceOrgUrl;
  private final String clientId;
  private final String clientSecret;
  private final String modelName;

  public EinsteinModel(EmbeddingConfiguration embeddingConfiguration, EinsteinModelConnection einsteinModelConnection, EmbeddingModelParameters embeddingModelParameters) {

    super(embeddingConfiguration, einsteinModelConnection, embeddingModelParameters);

    this.salesforceOrgUrl = einsteinModelConnection.getSalesforceOrg();
    this.clientId = einsteinModelConnection.getClientId();
    this.clientSecret = einsteinModelConnection.getClientSecret();
    this.modelName = embeddingModelParameters.getEmbeddingModelName();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return EinsteinEmbeddingModel.builder()
        .salesforceOrg(salesforceOrgUrl)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .modelName(modelName)
        .build();
  }
}
