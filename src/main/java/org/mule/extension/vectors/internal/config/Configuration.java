package org.mule.extension.vectors.internal.config;

import org.mule.extension.vectors.internal.helper.provider.EmbeddingModelServiceProvider;
import org.mule.extension.vectors.internal.helper.provider.VectorStoreProvider;
import org.mule.extension.vectors.internal.operation.DocumentOperations;
import org.mule.extension.vectors.internal.operation.EmbeddingOperations;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

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
  @OfValues(EmbeddingModelServiceProvider.class)
  private String embeddingModelService;

  @Parameter
  @Alias("vectorStore")
  @DisplayName("Vector Store")
  @Summary("The vector store.")
  @Placement(order = 2, tab = Placement.DEFAULT_TAB)
  @OfValues(VectorStoreProvider.class)
  private String vectorStore;

  @Parameter
  @Alias("configFilePath")
  @DisplayName("Configuration File Path")
  @Summary("The configuration file path.")
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
