package org.mule.extension.vectors.internal.model.multimodal.azureaivision;

import java.util.List;

public class AzureAIVisionEmbeddingResponseBody {

  private List<float[]> vectors;
  private String modelVersion;

  AzureAIVisionEmbeddingResponseBody() {
  }

  public List<float[]> getVectors() {
    return this.vectors;
  }

  public String getModelVersion() {
    return this.modelVersion;
  }
}
