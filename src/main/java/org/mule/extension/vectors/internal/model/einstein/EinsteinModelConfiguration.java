package org.mule.extension.vectors.internal.model.einstein;

import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.model.BaseModelConfiguration;
import org.mule.extension.vectors.internal.store.BaseStoreConfiguration;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

@Alias("einstein")
@DisplayName("Einstein")
public class EinsteinModelConfiguration implements BaseModelConfiguration {

  @Parameter
  @Alias("salesforceOrgUrl")
  @DisplayName("Salesforce Org URL")
  @Summary("The salesforce org base URL.")
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 1)
  private String salesforceOrgUrl;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 2)
  private String clientId;

  @Parameter
  @Password
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 3)
  private String clientSecret;

  @Override
  public String getEmbeddingModelService() {
    return Constants.EMBEDDING_MODEL_SERVICE_EINSTEIN;
  }

  public String getSalesforceOrgUrl() {
    return salesforceOrgUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }
}
