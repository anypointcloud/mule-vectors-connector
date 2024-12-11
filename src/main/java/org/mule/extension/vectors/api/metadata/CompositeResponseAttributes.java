package org.mule.extension.vectors.api.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompositeResponseAttributes implements Serializable {

  private final String fileType;
  private final String contextPath;
  private final String storeName;

  private final HashMap<String, Object> otherAttributes;

  public CompositeResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.fileType = requestAttributes.containsKey("fileType") ? (String)requestAttributes.remove("fileType") : null;
    this.contextPath = requestAttributes.containsKey("contextPath") ? (String)requestAttributes.remove("contextPath") : null;
    this.storeName = requestAttributes.containsKey("storeName") ? (String)requestAttributes.remove("storeName") : null;
    this.otherAttributes = requestAttributes;
  }

  public String getFileType() {
    return fileType;
  }

  public String getContextPath() {
    return contextPath;
  }

  public String getStoreName() {
    return storeName;
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }
}
