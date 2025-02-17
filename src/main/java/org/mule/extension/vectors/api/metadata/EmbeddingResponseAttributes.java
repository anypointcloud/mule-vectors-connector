package org.mule.extension.vectors.api.metadata;

import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

/**
 * Represents the attributes of an embedding operation response.
 * <p>
 * This class contains metadata about an embedding response, including the embedding model name,
 * its dimensions, and additional attributes.
 */
public class EmbeddingResponseAttributes implements Serializable {

  /**
   * The name of the embedding model used.
   */
  protected final String embeddingModelName;

  /**
   * The dimension of the embedding model.
   */
  protected final int embeddingModelDimension;

  protected final TokenUsage tokenUsage;

  /**
   * Additional attributes not explicitly defined as fields in this class.
   */
  protected final HashMap<String, Object> otherAttributes;

  /**
   * Constructs an {@code EmbeddingResponseAttributes} instance.
   *
   * @param requestAttributes a map containing attributes of the embedding operation response.
   *                          Expected keys include "embeddingModelName" and "embeddingModelDimension",
   *                          which are extracted and stored in their respective fields.
   *                          Remaining entries are stored in {@code otherAttributes}.
   */
  public EmbeddingResponseAttributes(HashMap<String, Object> requestAttributes) {
    this.embeddingModelName = requestAttributes.containsKey("embeddingModelName") ? (String) requestAttributes.remove("embeddingModelName") : null;
    this.embeddingModelDimension = requestAttributes.containsKey("embeddingModelDimension") ? (int) requestAttributes.remove("embeddingModelDimension") : null;
    this.tokenUsage = requestAttributes.containsKey("tokenUsage") ? (TokenUsage) requestAttributes.remove("tokenUsage") : null;
    this.otherAttributes = requestAttributes;
  }

  /**
   * Gets the name of the embedding model.
   *
   * @return the embedding model name, or {@code null} if not available.
   */
  public String getEmbeddingModelName() {
    return embeddingModelName;
  }

  /**
   * Gets the dimension of the embedding model.
   *
   * @return the embedding model dimension.
   */
  public int getEmbeddingModelDimension() {
    return embeddingModelDimension;
  }

  public TokenUsage getTokenUsage() { return tokenUsage; }

  /**
   * Gets additional attributes of the embedding response.
   * <p>
   * These are attributes not explicitly defined in this class.
   *
   * @return a map of additional attributes.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }
}
