/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.vectors.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONObject;

import org.mule.extension.vectors.internal.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public final class JsonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

  private JsonUtils() {}

  public static JSONObject readConfigFile(String filePath) {
    Path path = Paths.get(filePath);
    if (Files.exists(path)) {
      try {
        String content = new String(Files.readAllBytes(path));
        return new JSONObject(content);
      } catch (Exception e) {
        LOGGER.error("Unable to read the config file: " + filePath, e);
      }
    } else {
      LOGGER.warn("File does not exist: {}", filePath);
    }
    return null;
  }

  public static JsonNode stringToJsonNode(String content) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(content);
  }

  public static JSONArray jsonObjectCollectionToJsonArray(Collection<JSONObject> jsonObjectList) {
    JSONArray jsonArray = new JSONArray();
    for (JSONObject jsonObject : jsonObjectList) {
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

  /**
   * Creates a JSONObject representing the ingestion status of a folder or set of files.
   *
   * @return a JSONObject containing the ingestion status with file count, folder path, store name, and status.
   */
  public static JSONObject createIngestionStatusObject(String storeName) {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(Constants.JSON_KEY_STORE_NAME, storeName);
    jsonObject.put(Constants.JSON_KEY_STATUS, Constants.OPERATION_STATUS_UPDATED);
    return jsonObject;
  }
}
