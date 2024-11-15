package org.mule.extension.mulechain.vectors.internal.util;

import com.azure.core.implementation.logging.DefaultLogger;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.storage.s3.AWSS3Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utility class for adding metadata to Document instances.
 */
public class DocumentUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUtils.class);

  /**
   * Adds metadata to a Document with specified file type, file name, and file path.
   *
   * @param document the Document to which metadata is added.
   * @param fileType the type of the file (e.g., text, any).
   * @param fileName the name of the file.
   */
  public static void addMetadataToDocument(Document document, String fileType, String fileName) {

    document.metadata().put(Constants.METADATA_KEY_SOURCE_ID, dev.langchain4j.internal.Utils.randomUUID());
    document.metadata().put(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
    document.metadata().put(Constants.METADATA_KEY_INGESTION_TIMESTAMP, Utils.getCurrentTimeMillis());

    document.metadata().put(Constants.METADATA_KEY_FILE_TYPE, fileType);
    document.metadata().put(Constants.METADATA_KEY_FILE_NAME, fileName);

    if (fileType.equals(Constants.FILE_TYPE_CRAWL)) {

      try {

        JsonNode jsonNode = JsonUtils.stringToJsonNode(document.text());
        String source_url = jsonNode.path("url").asText();
        String title = jsonNode.path("title").asText();
        document.metadata().put(Constants.METADATA_KEY_URL, source_url);
        document.metadata().put(Constants.METADATA_KEY_TITLE, title);
      } catch (IOException ioe) {

        LOGGER.error(ioe.getMessage() + " " + Arrays.toString(ioe.getStackTrace()));
      }
    }
  }
}
