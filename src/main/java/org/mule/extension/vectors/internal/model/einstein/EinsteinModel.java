package org.mule.extension.vectors.internal.model.einstein;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.vectors.internal.config.Configuration;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.parameter.EmbeddingModelParameters;
import org.mule.extension.vectors.internal.model.BaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mule.extension.vectors.internal.util.JsonUtils.readConfigFile;

public class EinsteinModel  extends BaseModel {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinModel.class);

  private final String salesforceOrg;
  private final String clientId;
  private final String clientSecret;
  private final String modelName;

  public EinsteinModel(Configuration configuration, EmbeddingModelParameters embeddingModelParameters) {

    super(configuration,embeddingModelParameters);
    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    assert config != null;
    JSONObject modelConfig = config.getJSONObject(Constants.EMBEDDING_MODEL_SERVICE_EINSTEIN);
    this.salesforceOrg = modelConfig.getString("EINSTEIN_SFDC_ORG_URL");;
    this.clientId = modelConfig.getString("EINSTEIN_CLIENT_ID");;
    this.clientSecret = modelConfig.getString("EINSTEIN_CLIENT_SECRET");;
    this.modelName = embeddingModelParameters.getEmbeddingModelName();
  }

  public EmbeddingModel buildEmbeddingModel() {

    return EinsteinEmbeddingModel.builder()
        .salesforceOrg(salesforceOrg)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .modelName(modelName)
        .build();
  }
}
