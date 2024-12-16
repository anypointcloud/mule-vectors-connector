package org.mule.extension.vectors.api.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.mule.extension.vectors.internal.helper.parameter.MetadataFilterParameters;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StoreResponseAttributes implements Serializable {

  private final String storeName;
  private FilterAttribute filter;

  private final HashMap<String, Object> otherAttributes;

  public StoreResponseAttributes(HashMap<String, Object> requestAttributes) {

    this.storeName = requestAttributes.containsKey("storeName") ? (String)requestAttributes.remove("storeName") : null;
    MetadataFilterParameters filterParams = requestAttributes.containsKey("searchFilter") ?
        (MetadataFilterParameters.SearchFilterParameters)requestAttributes.remove("searchFilter") :
            requestAttributes.containsKey("removeFilter") ?
                (MetadataFilterParameters.RemoveFilterParameters)requestAttributes.remove("removeFilter") :
                null;
    if(filterParams != null) this.filter = new FilterAttribute(filterParams.getMetadataKey(), filterParams.getFilterMethod(), filterParams.getMetadataValue());
    this.otherAttributes = requestAttributes;
  }

  public String getStoreName() {
    return storeName;
  }

  public FilterAttribute getFilter() {return filter; }

  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }

  static class FilterAttribute {

    private String metadataKey;
    private String filterMethod;
    private Object metadataValue;

    public FilterAttribute(String metadataKey, String filterMethod, Object metadataValue) {
      this.metadataKey = metadataKey;
      this.filterMethod = filterMethod;
      this.metadataValue = metadataValue;
    }

    public String getMetadataKey() {
      return metadataKey;
    }

    public String getFilterMethod() {
      return filterMethod;
    }

    public Object getMetadataValue() {
      return metadataValue;
    }
  }
}
