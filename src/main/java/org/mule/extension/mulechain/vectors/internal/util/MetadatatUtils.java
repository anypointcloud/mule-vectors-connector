package org.mule.extension.mulechain.vectors.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * Utility class for setting metadata.
 */
public class MetadatatUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadatatUtils.class);

  public static void setBaseMetadata(Metadata metadata) {

    metadata.put(Constants.METADATA_KEY_SOURCE_ID, dev.langchain4j.internal.Utils.randomUUID());
    metadata.put(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
    metadata.put(Constants.METADATA_KEY_INGESTION_TIMESTAMP, Utils.getCurrentTimeMillis());
  }

  /**
   * Adds metadata to a Document with specified file type, file name, and file path.
   *
   * @param document the Document to which metadata is added.
   * @param fileType the type of the file (e.g., text, any).
   * @param fileName the name of the file.
   */
  public static void addMetadataToDocument(Document document, String fileType, String fileName) {

    setBaseMetadata(document.metadata());

    if(!fileType.isEmpty()) document.metadata().put(Constants.METADATA_KEY_FILE_TYPE, fileType);
    if(!fileName.isEmpty()) document.metadata().put(Constants.METADATA_KEY_FILE_NAME, fileName);

    if (fileType.equals(Constants.FILE_TYPE_CRAWL)) {

      try {

        JsonNode jsonNode = JsonUtils.stringToJsonNode(document.text());
        String source_url = jsonNode.path("url").asText();
        String title = jsonNode.path("title").asText();
        if(!source_url.isEmpty()) document.metadata().put(Constants.METADATA_KEY_URL, source_url);
        if(!title.isEmpty()) document.metadata().put(Constants.METADATA_KEY_TITLE, title);
      } catch (IOException ioe) {

        LOGGER.error(ioe.getMessage() + " " + Arrays.toString(ioe.getStackTrace()));
      }
    }
  }
}
