package org.mule.extension.mulechain.vectors.api.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentResponseAttributes implements Serializable {

  private final HashMap<String, Object> requestAttributes;

  public DocumentResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.requestAttributes = requestAttributes;
  }

  public Map<String, Object> getRequestAttributes() {
    return requestAttributes;
  }
}
