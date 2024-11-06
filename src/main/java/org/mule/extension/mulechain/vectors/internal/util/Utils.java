package org.mule.extension.mulechain.vectors.internal.util;

import java.io.File;
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
}
