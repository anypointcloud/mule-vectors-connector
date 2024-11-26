package org.mule.extension.vectors.internal.config;

import org.mule.extension.vectors.internal.model.BaseModelConfiguration;
import org.mule.extension.vectors.internal.operation.DocumentOperations;
import org.mule.extension.vectors.internal.operation.EmbeddingOperations;
import org.mule.extension.vectors.internal.store.BaseStoreConfiguration;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@org.mule.runtime.extension.api.annotation.Configuration(name = "config")
@Operations({EmbeddingOperations.class, DocumentOperations.class})
public class Configuration {

  @Parameter
  @Alias("embeddingModelService")
  @DisplayName("Embedding Model Service")
  @Summary("The embedding model service.")
  @Placement(order = 1, tab = Placement.DEFAULT_TAB)
  private BaseModelConfiguration modelConfiguration;

  @Parameter
  @Alias("vectorStore")
  @DisplayName("Vector Store")
  @Summary("The vector store.")
  @Placement(order = 2, tab = Placement.DEFAULT_TAB)
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  private BaseStoreConfiguration storeConfiguration;

  @Parameter
  @Alias("configFilePath")
  @DisplayName("Configuration File Path")
  @Summary("The configuration file path.")
  @Placement(order = 3, tab = Placement.DEFAULT_TAB)
  private String configFilePath;

  public BaseModelConfiguration getModelConfiguration() {
    return modelConfiguration;
  }

  public BaseStoreConfiguration getStoreConfiguration() {
    return storeConfiguration;
  }

  public String getConfigFilePath() {
    return configFilePath;
  }


}
