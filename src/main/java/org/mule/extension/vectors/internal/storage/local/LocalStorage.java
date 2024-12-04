package org.mule.extension.vectors.internal.storage.local;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.extension.vectors.internal.storage.BaseStorage;
import org.mule.extension.vectors.internal.util.MetadatatUtils;
import org.mule.extension.vectors.internal.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class LocalStorage extends BaseStorage {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorage.class);

  private final String fullPath;

  private List<Path> pathList;
  private Iterator<Path> pathIterator;

  private Iterator<Path> getPathIterator() {
    if (pathList == null) {  // Only load files if not already loaded
      try (Stream<Path> paths = Files.walk(Paths.get(fullPath))) {
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

  public LocalStorage(LocalStorageConfiguration localStorageConfiguration, String contextPath, String fileType) {

    super(localStorageConfiguration, contextPath, fileType);
    this.fullPath = dev.langchain4j.internal.Utils.getOrDefault(localStorageConfiguration.getWorkingDirectory(), "") +
        contextPath;
  }

  public LocalStorage(String contextPath, String fileType) {

    super(null, contextPath, fileType);
    this.fullPath = contextPath;
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
      MetadatatUtils.addMetadataToDocument(document, fileType, path.getFileName().toString());
      return document;
    }
    throw new IllegalStateException("No more files to iterate");
  }

  public Document getSingleDocument() {

    Path path = Paths.get(fullPath);

    DocumentParser documentParser = getDocumentParser(fileType);

    Document document;
    switch (fileType) {
      case Constants.FILE_TYPE_CRAWL:
      case Constants.FILE_TYPE_TEXT:
      case Constants.FILE_TYPE_ANY:
        document = loadDocument(path.toString(), documentParser);
        MetadatatUtils.addMetadataToDocument(document, fileType, Utils.getFileNameFromPath(fullPath));
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
      Document htmlDocument = UrlDocumentLoader.load(url, documentParser);
      HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
      document = transformer.transform(htmlDocument);
      document.metadata().put(Constants.METADATA_KEY_URL, contextPath);
      MetadatatUtils.addMetadataToDocument(document, Constants.FILE_TYPE_URL, "");
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + contextPath, e);
    }
    return document;
  }
}
