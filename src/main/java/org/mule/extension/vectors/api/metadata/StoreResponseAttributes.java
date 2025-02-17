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

/**
 * Represents the attributes of a store operation response.
 * <p>
 * This class contains metadata about a store, including the store name, filter attributes,
 * and additional attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StoreResponseAttributes implements Serializable {

  /**
   * The name of the store.
   */
  private final String storeName;

  /**
   * The filter attribute associated with the store response.
   */
  private FilterAttribute filter;

  /**
   * Additional attributes not explicitly defined as fields in this class.
   */
  private final HashMap<String, Object> otherAttributes;

  /**
   * Constructs a {@code StoreResponseAttributes} instance.
   *
   * @param requestAttributes a map containing attributes of the store operation response.
   *                          Expected keys include "storeName" and "searchFilter" or "removeFilter",
   *                          which are extracted and stored in their respective fields.
   *                          Remaining entries are stored in {@code otherAttributes}.
   */
  public StoreResponseAttributes(HashMap<String, Object> requestAttributes) {
    this.storeName = requestAttributes.containsKey("storeName") ? (String) requestAttributes.remove("storeName") : null;

    MetadataFilterParameters filterParams = requestAttributes.containsKey("searchFilter")
        ? (MetadataFilterParameters.SearchFilterParameters) requestAttributes.remove("searchFilter")
        : requestAttributes.containsKey("removeFilter")
            ? (MetadataFilterParameters.RemoveFilterParameters) requestAttributes.remove("removeFilter")
            : null;

    if (filterParams != null) {
      this.filter = new FilterAttribute(filterParams.getMetadataKey(), filterParams.getFilterMethod(), filterParams.getMetadataValue());
    }

    this.otherAttributes = requestAttributes;
  }

  /**
   * Gets the name of the store.
   *
   * @return the store name, or {@code null} if not available.
   */
  public String getStoreName() {
    return storeName;
  }

  /**
   * Gets the filter attribute associated with the store response.
   *
   * @return the filter attribute, or {@code null} if not available.
   */
  public FilterAttribute getFilter() {
    return filter;
  }

  /**
   * Gets additional attributes of the store response.
   * <p>
   * These are attributes not explicitly defined in this class.
   *
   * @return a map of additional attributes.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  public Map<String, Object> getOtherAttributes() {
    return otherAttributes;
  }

  /**
   * Represents the filter attribute of a store response.
   * <p>
   * A filter is defined by a metadata key, a filter method, and a metadata value.
   */
  static class FilterAttribute {

    /**
     * The key of the metadata used in the filter.
     */
    private String metadataKey;

    /**
     * The method applied to filter the metadata.
     */
    private String filterMethod;

    /**
     * The value of the metadata used in the filter.
     */
    private Object metadataValue;

    /**
     * Constructs a {@code FilterAttribute} instance.
     *
     * @param metadataKey    the key of the metadata used in the filter.
     * @param filterMethod   the filter method applied.
     * @param metadataValue  the value of the metadata used in the filter.
     */
    public FilterAttribute(String metadataKey, String filterMethod, Object metadataValue) {
      this.metadataKey = metadataKey;
      this.filterMethod = filterMethod;
      this.metadataValue = metadataValue;
    }

    /**
     * Gets the metadata key.
     *
     * @return the metadata key.
     */
    public String getMetadataKey() {
      return metadataKey;
    }

    /**
     * Gets the filter method.
     *
     * @return the filter method.
     */
    public String getFilterMethod() {
      return filterMethod;
    }

    /**
     * Gets the metadata value.
     *
     * @return the metadata value.
     */
    public Object getMetadataValue() {
      return metadataValue;
    }
  }
}
