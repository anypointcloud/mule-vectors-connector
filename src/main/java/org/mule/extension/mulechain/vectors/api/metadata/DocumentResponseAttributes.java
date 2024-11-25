package org.mule.extension.mulechain.vectors.api.metadata;

import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

public class DocumentResponseAttributes implements Serializable {

  private final HashMap<String, Object> requestAttributes;

  public DocumentResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.requestAttributes = requestAttributes;
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getRequestAttributes() {
    return requestAttributes;
  }
}
