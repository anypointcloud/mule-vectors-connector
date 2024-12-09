/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.vectors.internal.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.ValidationUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public final class JsonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

  private JsonUtils() {}

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
    jsonObject.put(Constants.JSON_KEY_STATUS, Constants.OPERATION_STATUS_UPDATED);
    return jsonObject;
  }

  public static JSONObject docToTextSegmentsJson(Document document, int maxSegmentSizeInChars, int maxOverlapSizeInChars) {

    List<TextSegment> textSegments;

    if(maxSegmentSizeInChars != 0 || maxOverlapSizeInChars != 0) {

      ValidationUtils.ensureGreaterThanZero(maxSegmentSizeInChars, "maxSegmentSizeInChars");
      ValidationUtils.ensureGreaterThanZero(maxOverlapSizeInChars, "maxOverlapSizeInChars");
      DocumentSplitter splitter = DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars);

      textSegments = splitter.split(document);
    } else {

      textSegments = Collections.singletonList(document.toTextSegment());
    }

    // Use Streams to populate a JSONArray
    JSONArray jsonTextSegments = IntStream.range(0, textSegments.size())
        .mapToObj(i -> {
          JSONObject jsonTextSegment = new JSONObject();
          jsonTextSegment.put(Constants.JSON_KEY_TEXT, textSegments.get(i).text());
          jsonTextSegment.put(Constants.JSON_KEY_METADATA, new JSONObject(textSegments.get(i).metadata().toMap()));
          return jsonTextSegment;
        })
        .collect(JSONArray::new, JSONArray::put, JSONArray::putAll);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(Constants.JSON_KEY_TEXT_SEGMENTS, jsonTextSegments);

    return jsonObject;
  }
}
