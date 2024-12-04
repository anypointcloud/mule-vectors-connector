package org.mule.extension.vectors.internal.connection.model.einstein;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.model.BaseModelConnectionProvider;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Alias("einstein")
@DisplayName("Einstein")
public class EinsteinModelConnectionProvider extends BaseModelConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(EinsteinModelConnectionProvider.class);

  public static final String URI_OAUTH_TOKEN = "/services/oauth2/token";
  public static final String QUERY_PARAM_GRANT_TYPE = "grant_type";
  public static final String QUERY_PARAM_CLIENT_ID = "client_id";
  public static final String QUERY_PARAM_CLIENT_SECRET = "client_secret";
  public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

  @ParameterGroup(name = Placement.CONNECTION_TAB)
  private EinsteinModelConnectionParameters einsteinModelConnectionParameters;

  @Override
  public BaseModelConnection connect() throws ConnectionException {

    LOGGER.debug(String.format("Connect to Salesforce. salesforceOrg: %s, clientId: %s, clientSecret: %s",
                               einsteinModelConnectionParameters.getSalesforceOrg(),
                               einsteinModelConnectionParameters.getClientId(),
                               einsteinModelConnectionParameters.getClientSecret()));

    try {

      EinsteinModelConnection einsteinModelConnection =
          new EinsteinModelConnection(einsteinModelConnectionParameters.getSalesforceOrg(),
                                      einsteinModelConnectionParameters.getClientId(),
                                      einsteinModelConnectionParameters.getClientSecret());

      einsteinModelConnection.connect();
      return einsteinModelConnection;

    } catch (ConnectionException e) {

      throw e;

    } catch (Exception e) {

      throw new ConnectionException("Failed to connect to Salesforce", e);
    }
  }

  @Override
  public void disconnect(BaseModelConnection connection) {

    try {

      connection.disconnect();
    } catch (Exception e) {

      LOGGER.error("Failed to close connection", e);
    }
  }

  @Override
  public ConnectionValidationResult validate(BaseModelConnection connection) {

    try {

      if (connection.isValid()) {
        return ConnectionValidationResult.success();
      } else {
        return ConnectionValidationResult.failure("Failed to validate connection to Einstein", null);
      }
    } catch (Exception e) {
      return ConnectionValidationResult.failure("Failed to validate connection to Einstein", e);
    }
  }
}
