package org.mule.extension.mulechain.vectors.internal.util;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility class for commonly used methods in the MAC Vectors connecotr.
 */
public class Utils {

  /**
   * Returns the current date and time in ISO 8601 format (UTC).
   * <p>
   * The format returned is compliant with the ISO 8601 standard, represented as:
   * {@code YYYY-MM-DDTHH:MM:SSZ}. This string representation uses UTC time zone,
   * ensuring consistency for timestamp-based metadata.
   * </p>
   *
   * @return A {@link String} representing the current date and time in ISO 8601 format.
   */
  public static String getCurrentISO8601Timestamp() {
    return DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());
  }

  /**
   * Returns the current time in milliseconds.
   *
   * @return The current time in milliseconds since the Unix epoch
   *         (January 1, 1970, 00:00:00 UTC).
   */
  public static long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  /**
   * Extracts the file name from a given file path.
   *
   * @param fullPath the full path of the file as a String
   * @return the name of the file, including the extension if present, as a String
   */
  public static String getFileNameFromPath(String fullPath) {
    File file = new File(fullPath);
    return file.getName();
  }

  /**
   * Returns the corresponding primitive type class for a given object value.
   *
   * This method checks the type of the provided object and returns the corresponding
   * primitive type class. If the object is an instance of {@link Integer}, {@link Long},
   * or {@link Double}, the method returns the respective primitive type class.
   * For unsupported types, it returns the class of the object.
   *
   * @param value The object to check and determine the corresponding primitive type class.
   * @return The primitive type class corresponding to the input value, or the object's class
   *         if it is not one of the supported types.
   */
  public static Class<?> getPrimitiveTypeClass(Object value) {
    if (value instanceof Integer) {
      return int.class;  // Return primitive int class
    } else if (value instanceof Long) {
      return long.class;  // Return primitive long class
    } else if (value instanceof Double) {
      return double.class;  // Return primitive double class
    } else {
      return value.getClass();  // Return class for unsupported types
    }
  }

  /**
   * Attempts to convert a given string into a specific object type.
   * <p>
   * This method attempts to parse the provided string into a {@link UUID}, {@link Integer},
   * {@link Long}, or {@link Double}, in that order. If none of these conversions are
   * successful, it returns the original string value.
   * </p>
   *
   * @param stringValue The string to be converted.
   * @return A {@link UUID}, {@link Integer}, {@link Long}, {@link Double}, or the original
   *         string depending on the successful conversion.
   */
  public static Object convertStringToType(String stringValue) {
    try {
      // Try to parse as UUID
      return UUID.fromString(stringValue);  // Returns a UUID object
    } catch (IllegalArgumentException e1) {
      try {
        // Try to parse as int
        return Integer.parseInt(stringValue);  // Returns Integer
      } catch (NumberFormatException e2) {
        try {
          // Try to parse as long
          return Long.parseLong(stringValue);  // Returns Long
        } catch (NumberFormatException e3) {
          try {
            // Try to parse as double
            return Double.parseDouble(stringValue);  // Returns Double
          } catch (NumberFormatException e4) {
            // If none of the above, return the string
            return stringValue;  // Keeps the value as a String
          }
        }
      }
    }
  }
}
