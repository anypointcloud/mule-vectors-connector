package org.mule.extension.mulechain.vectors.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.document.Document;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentUtils {

  public static void addMetadataToDocument(Path filePath, Document document) {
    try {
      String fileContent = new String(Files.readAllBytes(filePath));
      JsonNode jsonNode = JsonUtils.stringToJsonNode(fileContent.toString());
      String content = jsonNode.path("content").asText();
      String source_url = jsonNode.path("url").asText();
      String title = jsonNode.path("title").asText();
      document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
      document.metadata().add(Constants.METADATA_KEY_FILE_NAME, title);
      document.metadata().add(Constants.METADATA_KEY_FULL_PATH, source_url);
      document.metadata().put(Constants.METADATA_KEY_SOURCE, source_url);
      document.metadata().add(Constants.METADATA_KEY_TITLE, title);
      document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
    } catch (IOException e) {
      System.err.println("Error accessing folder: " + e.getMessage());
    }

  }

  public static void addMetadataToDocument(Document document) {
    try {
      String fileContent = document.text();
      JsonNode jsonNode = JsonUtils.stringToJsonNode(fileContent.toString());
      String content = jsonNode.path("content").asText();
      String source_url = jsonNode.path("url").asText();
      String title = jsonNode.path("title").asText();
      document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
      document.metadata().add(Constants.METADATA_KEY_FILE_NAME, title);
      document.metadata().add(Constants.METADATA_KEY_FULL_PATH, source_url);
      document.metadata().put(Constants.METADATA_KEY_SOURCE, source_url);
      document.metadata().add(Constants.METADATA_KEY_TITLE, title);
      document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
    } catch (IOException e) {
      System.err.println("Error accessing folder: " + e.getMessage());
    }

  }
}
