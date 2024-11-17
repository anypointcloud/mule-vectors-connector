package org.mule.extension.mulechain.vectors.internal.helper;

import org.mule.extension.mulechain.vectors.api.metadata.DocumentResponseAttributes;
import org.mule.extension.mulechain.vectors.api.metadata.EmbeddingResponseAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.IOUtils.toInputStream;

public final class ResponseHelper {

  private ResponseHelper() {
  }

  public static Result<InputStream, EmbeddingResponseAttributes> createEmbeddingResponse(
      String response,
      Map<String, String> embeddingAttributes) {

    return Result.<InputStream, EmbeddingResponseAttributes>builder()
        .attributes(new EmbeddingResponseAttributes((HashMap<String, String>) embeddingAttributes))
        .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
        .output(toInputStream(response, StandardCharsets.UTF_8))
        .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
        .build();
  }

  public static Result<InputStream, DocumentResponseAttributes> createDocumentResponse(
      String response,
      Map<String, String> documentAttributes) {

    return Result.<InputStream, DocumentResponseAttributes>builder()
        .attributes(new DocumentResponseAttributes((HashMap<String, String>) documentAttributes))
        .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
        .output(toInputStream(response, StandardCharsets.UTF_8))
        .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
        .build();
  }
}
