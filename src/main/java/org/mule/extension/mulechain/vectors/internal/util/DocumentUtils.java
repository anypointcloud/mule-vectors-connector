package org.mule.extension.mulechain.vectors.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for adding metadata to Document instances.
 */
public class DocumentUtils {

  /**
   * Adds metadata to a Document with specified file type, file name, and file path.
   *
   * @param document the Document to which metadata is added.
   * @param fileType the type of the file (e.g., text, any).
   * @param fileName the name of the file.
   * @param filePath the path of the file.
   */
  public static void addMetadataToDocument(Document document, String fileType, String fileName, String filePath) {

    document.metadata().add(Constants.METADATA_KEY_SOURCE_ID, dev.langchain4j.internal.Utils.randomUUID());
    document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, fileType);
    document.metadata().add(Constants.METADATA_KEY_FILE_NAME, fileName);
    document.metadata().add(Constants.METADATA_KEY_FULL_PATH, filePath);
    document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
  }

  /**
   * Adds metadata to a Document based on a file path, extracting details from the file content.
   * Adds fields such as the file type, title, and source URL if available.
   *
   * @param document the Document to which metadata is added.
   * @param fileType the type of the file.
   * @param filePath the path to the file whose content is read to extract metadata.
   */
  public static void addMetadataToDocument(Document document, String fileType, Path filePath) {
    try {
      String fileContent = new String(Files.readAllBytes(filePath));
      JsonNode jsonNode = JsonUtils.stringToJsonNode(fileContent.toString());
      String source_url = jsonNode.path("url").asText();
      String title = jsonNode.path("title").asText();
      addMetadataToDocument(document, fileType, title, source_url);
      document.metadata().put(Constants.METADATA_KEY_SOURCE, source_url);
      document.metadata().add(Constants.METADATA_KEY_TITLE, title);
    } catch (IOException e) {
      System.err.println("Error accessing folder: " + e.getMessage());
    }

  }

  /**
   * Adds metadata to a Document by analyzing its text content, extracting details such as title and source URL.
   * Default file type is used.
   *
   * @param document the Document to which metadata is added.
   */
  public static void addMetadataToDocument(Document document) {
    try {
      String fileContent = document.text();
      JsonNode jsonNode = JsonUtils.stringToJsonNode(fileContent.toString());
      String source_url = jsonNode.path("url").asText();
      String title = jsonNode.path("title").asText();
      addMetadataToDocument(document, Constants.FILE_TYPE_TEXT, title, source_url);
      document.metadata().put(Constants.METADATA_KEY_SOURCE, source_url);
      document.metadata().add(Constants.METADATA_KEY_TITLE, title);
    } catch (IOException e) {
      System.err.println("Error accessing folder: " + e.getMessage());
    }

  }
}
