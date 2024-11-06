package org.mule.extension.mulechain.vectors.internal.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
}
