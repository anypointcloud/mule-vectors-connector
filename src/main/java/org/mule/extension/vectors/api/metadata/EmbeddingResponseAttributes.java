package org.mule.extension.vectors.api.metadata;

import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class EmbeddingResponseAttributes  implements Serializable {

  private final String embeddingModelName;
  private final int embeddingModelDimension;

  private final HashMap<String, Object> otherAttributes;

  public EmbeddingResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.embeddingModelName = requestAttributes.containsKey("embeddingModelName") ? (String)requestAttributes.remove("embeddingModelName") : null;
    this.embeddingModelDimension = requestAttributes.containsKey("embeddingModelDimension") ? (int)requestAttributes.remove("embeddingModelDimension") : null;
    this.otherAttributes = requestAttributes;
  }

  public String getEmbeddingModelName() {
    return embeddingModelName;
  }

  public int getEmbeddingModelDimension() {
    return embeddingModelDimension;
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }
}
