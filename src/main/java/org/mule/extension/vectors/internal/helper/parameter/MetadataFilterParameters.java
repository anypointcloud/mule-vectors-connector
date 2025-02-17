package org.mule.extension.vectors.internal.helper.parameter;

import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.json.JSONObject;

import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.helper.provider.MetadataFilterMethodProvider;
import org.mule.extension.vectors.internal.helper.provider.MetadataKeyProvider;
import org.mule.extension.vectors.internal.util.Utils;
import org.mule.runtime.api.meta.ExpressionSupport;

import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import dev.langchain4j.store.embedding.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Abstract class that defines filter parameters for MuleChain Vectors.
 * Provides methods to validate parameters and build a metadata filter.
 */
public abstract class MetadataFilterParameters {

  protected static final Logger LOGGER = LoggerFactory.getLogger(MetadataFilterParameters.class);

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
  public abstract Object getMetadataValue();

  public abstract boolean isMetadataKeyEmpty();

  public abstract boolean isFilterMethodEmpty();

  public abstract boolean isMetadataValueEmpty();

  /**
   * Validates if all filter parameters (metadata key, filter method, metadata value) are set.
   *
   * @return {@code true} if all filter parameters are set; {@code false} otherwise.
   */
  public boolean areFilterParamsSet() {
    return !isMetadataKeyEmpty() && !isFilterMethodEmpty() && !isMetadataValueEmpty();
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

    try {
      // Initialize the filter builder with metadataKey
      MetadataFilterBuilder filterBuilder = metadataKey(getMetadataKey());

      // Get the method name to call (e.g., "isGreaterThan")
      String methodName = getFilterMethod();

      // Get the value and determine its type
      Object metadataValue = getMetadataValue();
      Class<?> parameterType = metadataValue.getClass();

      // Log the metadata value type for debugging
      LOGGER.debug("Metadata value type: " + parameterType.getName());

      // Get the method with the correct name and parameter type
      Method method = filterBuilder.getClass().getMethod(methodName, Utils.getPrimitiveTypeClass(metadataValue));

      // Dynamically invoke the method with the metadata value as an argument
      filter = (Filter) method.invoke(filterBuilder, metadataValue);

    } catch (NoSuchMethodException nsme) {

      LOGGER.error(nsme.getMessage() + " " + Arrays.toString(nsme.getStackTrace()));
      throw new IllegalArgumentException("Filter method doesn't exist.");

    } catch (IllegalArgumentException iae) {

      LOGGER.error(iae.getMessage() + " " + Arrays.toString(iae.getStackTrace()));
      throw iae;

    } catch (Exception e) {

      LOGGER.error(e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
      throw new IllegalArgumentException("IllegalArgumentException. Impossible to define the filter");
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
  public static class SearchFilterParameters extends MetadataFilterParameters {

    /**
     * The metadata key used for filtering in search operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MetadataKeyProvider.class)
    @Summary("The metadata key used for filtering")
    @Optional
    private String metadataKey;

    /**
     * The filtering method, defaulting to "equalTo" if not specified.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MetadataFilterMethodProvider.class)
    @Summary("The method used to apply the filter, e.g., isEqualsTo or notEqualsTo")
    @Optional(defaultValue= Constants.METADATA_FILTER_METHOD_IS_EQUAL_TO)
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
    public Object getMetadataValue() {

      return metadataValue != null && !metadataValue.isEmpty() ? Utils.convertStringToType(metadataValue) : metadataValue;
    }

    @Override
    public boolean isMetadataKeyEmpty() { return metadataKey == null || metadataKey.isEmpty(); }

    @Override
    public boolean isFilterMethodEmpty() { return filterMethod == null || filterMethod.isEmpty(); }

    @Override
    public boolean isMetadataValueEmpty() { return metadataValue == null || metadataValue.isEmpty(); }
  }

  /**
   * Inner class representing the filter parameters for remove operations.
   */
  public static class RemoveFilterParameters extends MetadataFilterParameters {

    /**
     * The metadata key used for filtering in remove operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MetadataKeyProvider.class)
    @Summary("The metadata key used for filtering")
    private String metadataKey;

    /**
     * The filtering method used in remove operations.
     */
    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @OfValues(MetadataFilterMethodProvider.class)
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
    public Object getMetadataValue() {

      return metadataValue != null && !metadataValue.isEmpty() ? Utils.convertStringToType(metadataValue) : metadataValue;
    }

    @Override
    public boolean isMetadataKeyEmpty() { return metadataKey == null || metadataKey.isEmpty(); }

    @Override
    public boolean isFilterMethodEmpty() { return filterMethod == null || filterMethod.isEmpty(); }

    @Override
    public boolean isMetadataValueEmpty() { return metadataValue == null || metadataValue.isEmpty(); }
  }
}
