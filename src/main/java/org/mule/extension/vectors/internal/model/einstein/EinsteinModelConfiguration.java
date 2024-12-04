package org.mule.extension.vectors.internal.model.einstein;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.model.einstein.EinsteinModelConnection;
import org.mule.extension.vectors.internal.model.BaseModelConfiguration;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.*;

@Alias("einstein")
@DisplayName("Einstein")
public class EinsteinModelConfiguration implements BaseModelConfiguration {

  @Parameter
  @Alias("salesforceOrgUrl")
  @DisplayName("Salesforce Org URL")
  @Summary("The salesforce org base URL.")
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 1)
  @Example("mydomain.my.salesforce.com")
  private String salesforceOrg;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 2)
  @Example("<your-connected-app-client-id>")
  private String clientId;

  @Parameter
  @Password
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 3)
  @Example("<your-connected-app-client-secret>")
  private String clientSecret;

  @Override
  public BaseModelConnection getConnection() {

    return new EinsteinModelConnection(salesforceOrg, clientId, clientSecret);
  }

  public String getSalesforceOrg() {
    return salesforceOrg;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }
}
