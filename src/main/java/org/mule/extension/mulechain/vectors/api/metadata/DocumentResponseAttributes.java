package org.mule.extension.mulechain.vectors.api.metadata;

import java.io.Serializable;
import java.util.HashMap;

public class DocumentResponseAttributes implements Serializable {

  private final HashMap<String, String> documentAttributes;

  public DocumentResponseAttributes(HashMap<String, String> documentAttributes) {

    this.documentAttributes = documentAttributes;
  }
}
