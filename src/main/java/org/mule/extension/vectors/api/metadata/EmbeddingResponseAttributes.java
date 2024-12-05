package org.mule.extension.vectors.api.metadata;

import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class EmbeddingResponseAttributes  implements Serializable {

  private final String embeddingModelName;

  private final HashMap<String, Object> otherAttributes;

  public EmbeddingResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.embeddingModelName = (String)requestAttributes.remove("embeddingModelName");
    this.otherAttributes = requestAttributes;
  }

  public String getEmbeddingModelName() {
    return embeddingModelName;
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }
}
