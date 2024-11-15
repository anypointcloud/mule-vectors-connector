package org.mule.extension.mulechain.vectors.internal.storage.local;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.storage.BaseStorage;
import org.mule.extension.mulechain.vectors.internal.storage.s3.AWSS3Storage;
import org.mule.extension.mulechain.vectors.internal.util.DocumentUtils;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.mule.extension.mulechain.vectors.internal.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class LocalStorage extends BaseStorage {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorage.class);

  private List<Path> pathList;
  private Iterator<Path> pathIterator;

  private Iterator<Path> getPathIterator() {
    if (pathList == null) {  // Only load files if not already loaded
      try (Stream<Path> paths = Files.walk(Paths.get(contextPath))) {
        // Collect all files as a list
        pathList = paths.filter(Files::isRegularFile).collect(Collectors.toList());
        // Create an iterator for the list of files
        pathIterator = pathList.iterator();
      } catch (IOException e) {
        LOGGER.error("Error processing files: ", e);
      }
    }
    return pathIterator;
  }

  public LocalStorage(Configuration configuration, String contextPath, String fileType) {

    super(configuration, contextPath, fileType);
  }

  // Override hasNext to check if there are files left to process
  @Override
  public boolean hasNext() {
    return getPathIterator() != null && getPathIterator().hasNext();
  }

  // Override next to return the next document
  @Override
  public Document next() {
    if (hasNext()) {
      Path path = getPathIterator().next();
      LOGGER.debug("File: " + path.getFileName().toString());
      Document document = loadDocument(path.toString(), documentParser);
      DocumentUtils.addMetadataToDocument(document, fileType, path.getFileName().toString());
      return document;
    }
    throw new IllegalStateException("No more files to iterate");
  }

  public Document getSingleDocument() {

    Path path = Paths.get(contextPath);

    DocumentParser documentParser = getDocumentParser(fileType);

    Document document;
    switch (fileType) {
      case Constants.FILE_TYPE_CRAWL:
      case Constants.FILE_TYPE_TEXT:
      case Constants.FILE_TYPE_ANY:
        document = loadDocument(path.toString(), documentParser);
        DocumentUtils.addMetadataToDocument(document, fileType, Utils.getFileNameFromPath(contextPath));
        break;
      case Constants.FILE_TYPE_URL:
        document = loadUrlDocument(contextPath);
        break;
      default:
        throw new IllegalArgumentException("Unsupported File Type: " + fileType);
    }
    return document;
  }

  private Document loadUrlDocument(String contextPath) {

    Document document;
    try {
      URL url = new URL(contextPath);
      Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
      HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
      document = transformer.transform(htmlDocument);
      document.metadata().put(Constants.METADATA_KEY_URL, contextPath);
      DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_URL, "");
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + contextPath, e);
    }
    return document;
  }
}
