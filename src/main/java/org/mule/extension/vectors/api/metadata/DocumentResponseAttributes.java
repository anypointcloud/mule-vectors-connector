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
public class DocumentResponseAttributes implements Serializable {

  private final String fileType;
  private final String contextPath;

  private final HashMap<String, Object> otherAttributes;

  public DocumentResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.fileType = (String)requestAttributes.remove("fileType");
    this.contextPath = (String)requestAttributes.remove("contextPath");
    this.otherAttributes = requestAttributes;
  }

  public String getFileType() {
    return fileType;
  }

  public String getContextPath() {
    return contextPath;
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }
}
