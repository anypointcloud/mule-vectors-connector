package org.mule.extension.vectors.internal.pagination;

import dev.langchain4j.data.document.Document;
import org.json.JSONObject;
import org.mule.extension.vectors.api.metadata.DocumentResponseAttributes;
import org.mule.extension.vectors.internal.config.DocumentConfiguration;
import org.mule.extension.vectors.internal.connection.storage.BaseStorageConnection;
import org.mule.extension.vectors.internal.helper.parameter.DocumentParameters;
import org.mule.extension.vectors.internal.helper.parameter.SegmentationParameters;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.util.JsonUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.extension.vectors.internal.helper.ResponseHelper.createPageDocumentResponse;

public class DocumentPagingProvider implements PagingProvider<BaseStorageConnection, Result<CursorProvider, DocumentResponseAttributes>> {

  private StreamingHelper streamingHelper;
  private BaseStorage baseStorage;
  private DocumentConfiguration documentConfiguration;
  private DocumentParameters documentParameters;
  private SegmentationParameters segmentationParameters;

  public DocumentPagingProvider(DocumentConfiguration documentConfiguration, DocumentParameters documentParameters,
                                SegmentationParameters segmentationParameters, StreamingHelper streamingHelper) {

    this.documentConfiguration = documentConfiguration;
    this.documentParameters = documentParameters;
    this.segmentationParameters = segmentationParameters;
    this.streamingHelper = streamingHelper;
  }

  @Override
  public List<Result<CursorProvider, DocumentResponseAttributes>> getPage(BaseStorageConnection connection) {

    if(baseStorage == null) {

      baseStorage = BaseStorage.builder()
          .configuration(documentConfiguration)
          .connection(connection)
          .contextPath(documentParameters.getContextPath())
          .fileType(documentParameters.getFileType())
          .build();
    }

    if(baseStorage.hasNext()) {

      Document document = baseStorage.next();

      JSONObject jsonObject =
          JsonUtils.docToTextSegmentsJson(document,
                                          segmentationParameters.getMaxSegmentSizeInChars(),
                                          segmentationParameters.getMaxOverlapSizeInChars());

      return createPageDocumentResponse(
          jsonObject.toString(),
          new HashMap<String, Object>() {{
            put("fileType", documentParameters.getFileType());
            put("contextPath", documentParameters.getContextPath());
          }},
          streamingHelper
      );

    } else {

      return Collections.emptyList();
    }
  }

  @Override
  public Optional<Integer> getTotalResults(BaseStorageConnection connection) {
    return java.util.Optional.empty();
  }

  @Override
  public void close(BaseStorageConnection connection) throws MuleException {

  }

  @Override
  public boolean useStickyConnections() {
    return true;
  }
}
