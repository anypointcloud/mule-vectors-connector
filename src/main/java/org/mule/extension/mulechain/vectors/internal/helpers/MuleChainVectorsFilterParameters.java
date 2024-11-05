package org.mule.extension.mulechain.vectors.internal.helpers;

import org.json.JSONObject;

import org.mule.runtime.api.meta.ExpressionSupport;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import dev.langchain4j.store.embedding.filter.Filter;

/**
 * Abstract class that defines filter parameters for MuleChain Vectors.
 * Provides methods to validate parameters and build a metadata filter.
 */
public abstract class MuleChainVectorsFilterParameters {

  /**
   * @return The metadata key to be used for filtering.
   */
  public abstract String getMetadataKey();

  /**
   * @return The filtering method to be applied, such as "equalsTo" or "notEqualsTo".
   */
  public abstract String getFilterMethod();

  /**
   * @return The metadata value to be used in the filtering operation.
   */
  public abstract String getMetadataValue();

  /**
   * Validates if all filter parameters (metadata key, filter method, metadata value) are set.
   *
   * @return {@code true} if all filter parameters are set; {@code false} otherwise.
   */
  public boolean areFilterParamsSet() {
    return getMetadataKey() != null && !getMetadataKey().isEmpty() &&
            getFilterMethod() != null && !getFilterMethod().isEmpty() &&
            getMetadataValue() != null && !getMetadataValue().isEmpty();
  }

  /**
   * Builds a metadata filter based on the provided filter parameters.
   * Throws an {@link IllegalArgumentException} if any parameter is missing.
   *
   * @return A {@link Filter} object constructed based on the specified filter method.
   * @throws IllegalArgumentException if any filter parameter is missing.
   */
  public Filter buildMetadataFilter() {
    if (!areFilterParamsSet()) {
      throw new IllegalArgumentException("Filter parameters are not set. Please provide metadataKey, filterMethod, and metadataValue.");
    }

    Filter filter;
    switch(getFilterMethod()) {

      case MuleChainVectorsMetadataFilterMethodProvider.IS_EQUAL_TO:
        filter = metadataKey(getMetadataKey()).isEqualTo(getMetadataValue());
        break;

      case MuleChainVectorsMetadataFilterMethodProvider.IS_NOT_EQUAL_TO:
        filter = metadataKey(getMetadataKey()).isNotEqualTo(getMetadataValue());
        break;

      default:
        throw new IllegalArgumentException("Filter method is not set or an invalid value has been provided.");
    }
    return filter;
  }

  /**
   * Generates a string describing the filter in the format:
   * "Metadata Key: [key], Filter Method: [method], Metadata Value: [value]".
   *
   * @return A descriptive string summarizing the filter.
   */
  public String getFilterDescription() {
    return String.format("Metadata Key: %s, Filter Method: %s, Metadata Value: %s",
            getMetadataKey(), getFilterMethod(), getMetadataValue());
  }

  /**
   * Returns a JSON representation of the filter with keys "metadataKey", "filterMethod", and "metadataValue".
   *
   * @return A {@link JSONObject} representing the filter parameters in JSON format.
   */
  public JSONObject getFilterJSONObject() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("metadataKey", getMetadataKey());
    jsonObject.put("filterMethod", getFilterMethod());
    jsonObject.put("metadataValue", getMetadataValue());
    return jsonObject;
  }

  /**
   * Inner class representing the filter parameters for search operations.
   */
  public static class SearchFilterParameters extends MuleChainVectorsFilterParameters {

    /**
     * The metadata key used for filtering in search operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MuleChainVectorsMetadataKeyProvider.class)
    @Summary("The metadata key used for filtering")
    @Optional
    private String metadataKey;

    /**
     * The filtering method, defaulting to "equalTo" if not specified.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MuleChainVectorsMetadataFilterMethodProvider.class)
    @Summary("The method used to apply the filter, e.g., equalsTo or notEqualsTo")
    @Optional(defaultValue="equalTo")
    private String filterMethod;

    /**
     * The metadata value used for filtering.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Summary("The metadata value to be used in the filter operation")
    @Optional
    private String metadataValue;

    @Override
    public String getMetadataKey() {
      return metadataKey;
    }

    @Override
    public String getFilterMethod() {
      return filterMethod;
    }

    @Override
    public String getMetadataValue() {
      return metadataValue;
    }
  }

  /**
   * Inner class representing the filter parameters for remove operations.
   */
  public static class RemoveFilterParameters extends MuleChainVectorsFilterParameters {

    /**
     * The metadata key used for filtering in remove operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MuleChainVectorsMetadataKeyProvider.class)
    @Summary("The metadata key used for filtering")
    private String metadataKey;

    /**
     * The filtering method used in remove operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MuleChainVectorsMetadataFilterMethodProvider.class)
    @Summary("The method used to apply the filter, e.g., equalsTo or notEqualsTo")
    private String filterMethod;

    /**
     * The metadata value used for filtering in remove operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Summary("The metadata value to be used in the filter operation")
    private String metadataValue;

    @Override
    public String getMetadataKey() {
      return metadataKey;
    }

    @Override
    public String getFilterMethod() {
      return filterMethod;
    }

    @Override
    public String getMetadataValue() {
      return metadataValue;
    }
  }
}
