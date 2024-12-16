package org.mule.extension.vectors.api.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

/**
 * Represents the attributes of a composite operation response.
 * <p>
 * This class contains metadata about a composite response, including file type, context path,
 * store name, and additional attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompositeResponseAttributes implements Serializable {

  /**
   * The type of the file associated with the composite response.
   */
  private final String fileType;

  /**
   * The context path of the composite response.
   */
  private final String contextPath;

  /**
   * The name of the store associated with the composite response.
   */
  private final String storeName;

  /**
   * Additional attributes not explicitly defined as fields in this class.
   */
  private final HashMap<String, Object> otherAttributes;

  /**
   * Constructs a {@code CompositeResponseAttributes} instance.
   *
   * @param requestAttributes a map containing attributes of the composite operation response.
   *                          Expected keys include "fileType", "contextPath", and "storeName",
   *                          which are extracted and stored in their respective fields.
   *                          Remaining entries are stored in {@code otherAttributes}.
   */
  public CompositeResponseAttributes(HashMap<String, Object> requestAttributes) {
    this.fileType = requestAttributes.containsKey("fileType") ? (String) requestAttributes.remove("fileType") : null;
    this.contextPath = requestAttributes.containsKey("contextPath") ? (String) requestAttributes.remove("contextPath") : null;
    this.storeName = requestAttributes.containsKey("storeName") ? (String) requestAttributes.remove("storeName") : null;
    this.otherAttributes = requestAttributes;
  }

  /**
   * Gets the file type of the composite response.
   *
   * @return the file type, or {@code null} if not available.
   */
  public String getFileType() {
    return fileType;
  }

  /**
   * Gets the context path of the composite response.
   *
   * @return the context path, or {@code null} if not available.
   */
  public String getContextPath() {
    return contextPath;
  }

  /**
   * Gets the store name of the composite response.
   *
   * @return the store name, or {@code null} if not available.
   */
  public String getStoreName() {
    return storeName;
  }

  /**
   * Gets additional attributes of the composite response.
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
