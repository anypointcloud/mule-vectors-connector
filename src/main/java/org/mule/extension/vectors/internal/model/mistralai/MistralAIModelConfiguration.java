package org.mule.extension.vectors.internal.model.mistralai;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.model.mistralai.MistralAIModelConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.model.BaseModelConfiguration;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

@Alias("mistralAI")
@DisplayName("Mistral AI")
public class MistralAIModelConfiguration implements BaseModelConfiguration {

  @Parameter
  @Password
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 1)
  @Example("<your-api-key>")
  private String apiKey;

  @Override
  public BaseModelConnection getConnection() {

    return new MistralAIModelConnection(apiKey);
  }

  public String getApiKey() {
    return apiKey;
  }
}
