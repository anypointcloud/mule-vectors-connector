package org.mule.extension.mulechain.vectors.internal.config;

import org.mule.extension.mulechain.vectors.internal.helpers.MuleChainVectorsEmbeddingModelServiceProvider;
import org.mule.extension.mulechain.vectors.internal.helpers.MuleChainVectorsStoreTypeProvider;
import org.mule.extension.mulechain.vectors.internal.operation.MuleChainVectorsOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name = "config")
@Operations(MuleChainVectorsOperations.class)
public class MuleChainVectorsConfiguration {

  @Parameter
  @Placement(order = 1, tab = Placement.DEFAULT_TAB)
  @OfValues(MuleChainVectorsEmbeddingModelServiceProvider.class)
  private String embeddingModelService;

  @Parameter
  @Placement(order = 2, tab = Placement.DEFAULT_TAB)
  @OfValues(MuleChainVectorsStoreTypeProvider.class)
  private String vectorDBProviderType;

  @Parameter
  @Placement(order = 3, tab = Placement.DEFAULT_TAB)
  private String configFilePath;

  public String getEmbeddingModelService() {
    return embeddingModelService;
  }

  public String getVectorDBProviderType() {
    return vectorDBProviderType;
  }

  public String getConfigFilePath() {
    return configFilePath;
  }

}
