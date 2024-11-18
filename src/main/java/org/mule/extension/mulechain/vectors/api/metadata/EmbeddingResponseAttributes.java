package org.mule.extension.mulechain.vectors.api.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EmbeddingResponseAttributes  implements Serializable {

  private final HashMap<String, Object> requestAttributes;

  public EmbeddingResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.requestAttributes = requestAttributes;
  }

  public Map<String, Object> getRequestAttributes() {
    return requestAttributes;
  }
}
