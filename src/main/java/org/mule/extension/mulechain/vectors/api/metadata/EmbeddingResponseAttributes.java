package org.mule.extension.mulechain.vectors.api.metadata;

import java.io.Serializable;
import java.util.HashMap;

public class EmbeddingResponseAttributes  implements Serializable {

  private final HashMap<String, String> embeddingAttributes;

  public EmbeddingResponseAttributes(HashMap<String, String> embeddingAttributes) {

    this.embeddingAttributes = embeddingAttributes;
  }
}
