package org.mule.extension.mulechain.vectors.internal.config;

import org.mule.extension.mulechain.vectors.internal.helper.provider.EmbeddingModelServiceProvider;
import org.mule.extension.mulechain.vectors.internal.helper.provider.VectorStoreProvider;
import org.mule.extension.mulechain.vectors.internal.operation.DocumentOperations;
import org.mule.extension.mulechain.vectors.internal.operation.EmbeddingOperations;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@org.mule.runtime.extension.api.annotation.Configuration(name = "config")
@Operations({EmbeddingOperations.class, DocumentOperations.class})
public class Configuration {

  @Parameter
  @Placement(order = 1, tab = Placement.DEFAULT_TAB)
  @OfValues(EmbeddingModelServiceProvider.class)
  private String embeddingModelService;

  @Parameter
  @Placement(order = 2, tab = Placement.DEFAULT_TAB)
  @OfValues(VectorStoreProvider.class)
  private String vectorStore;

  @Parameter
  @Placement(order = 3, tab = Placement.DEFAULT_TAB)
  private String configFilePath;

  public String getEmbeddingModelService() {
    return embeddingModelService;
  }

  public String getVectorStore() {
    return vectorStore;
  }

  public String getConfigFilePath() {
    return configFilePath;
  }

}
