package org.mule.extension.vectors.internal.model.text.einstein;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mule.extension.vectors.internal.config.EmbeddingConfiguration;
import org.mule.extension.vectors.internal.connection.model.einstein.EinsteinModelConnection;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EinsteinModel  extends BaseModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinModel.class);

  private final String modelName;
  private final EinsteinModelConnection einsteinModelConnection;

  public EinsteinModel(EmbeddingConfiguration embeddingConfiguration, EinsteinModelConnection einsteinModelConnection, EmbeddingModelParameters embeddingModelParameters) {

    super(embeddingConfiguration, einsteinModelConnection, embeddingModelParameters);

    this.einsteinModelConnection = einsteinModelConnection;
    this.modelName = embeddingModelParameters.getEmbeddingModelName();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return EinsteinEmbeddingModel.builder()
        .connection(einsteinModelConnection)
        .modelName(modelName)
        .build();
  }
}
