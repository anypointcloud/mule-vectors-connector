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
 * Represents the attributes of a document operation response.
 * <p>
 * This class contains metadata about a document, such as its file type,
 * context path, and any additional attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DocumentResponseAttributes implements Serializable {

  /**
   * The type of the file associated with the document.
   */
  private final String fileType;

  /**
   * The context path of the document.
   */
  private final String contextPath;

  /**
   * Additional attributes not explicitly defined as fields in this class.
   */
  private final HashMap<String, Object> otherAttributes;

  /**
   * Constructs a {@code DocumentResponseAttributes} instance.
   *
   * @param requestAttributes a map containing document operation attributes.
   *                          Expected keys include "fileType" and "contextPath",
   *                          which are extracted and stored in their respective fields.
   *                          Remaining entries are stored in {@code otherAttributes}.
   */
  public DocumentResponseAttributes(HashMap<String, Object> requestAttributes) {
    this.fileType = requestAttributes.containsKey("fileType") ? (String) requestAttributes.remove("fileType") : null;
    this.contextPath = requestAttributes.containsKey("contextPath") ? (String) requestAttributes.remove("contextPath") : null;
    this.otherAttributes = requestAttributes;
  }

  /**
   * Gets the file type of the document.
   *
   * @return the file type, or {@code null} if not available.
   */
  public String getFileType() {
    return fileType;
  }

  /**
   * Gets the context path of the document.
   *
   * @return the context path, or {@code null} if not available.
   */
  public String getContextPath() {
    return contextPath;
  }

  /**
   * Gets additional attributes of the document.
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
