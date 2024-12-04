package org.mule.extension.vectors.internal.model.azureopenai;

import org.mule.extension.vectors.internal.connection.model.BaseModelConnection;
import org.mule.extension.vectors.internal.connection.model.azureopenai.AzureOpenAIModelConnection;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.connection.storage.amazons3.AmazonS3StorageConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.model.BaseModelConfiguration;
import org.mule.extension.vectors.internal.store.BaseStoreConfiguration;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

@Alias("azureOpenAI")
@DisplayName("Azure OpenAI")
public class AzureOpenAIModelConfiguration implements BaseModelConfiguration {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 1)
  @Example("https://<resource-name>.openai.azure.com")
  private String endpoint;

  @Parameter
  @Password
  @Expression(ExpressionSupport.SUPPORTED)
  @Placement(order = 2)
  @Example("<your-api-key>")
  private String apiKey;

  @Override
  public BaseModelConnection getConnection() {

    return new AzureOpenAIModelConnection(endpoint, apiKey);
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getApiKey() {
    return apiKey;
  }
}
