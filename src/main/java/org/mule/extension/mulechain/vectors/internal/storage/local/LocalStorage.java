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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class LocalStorage extends BaseStorage {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorage.class);

  public LocalStorage(Configuration configuration, String storeName, EmbeddingStoreIngestor embeddingStoreIngestor) {

    super(configuration, storeName, embeddingStoreIngestor);
  }

  public JSONObject readAndIngestAllFiles(String folderPath, String fileType) {

    long totalFiles = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      totalFiles = paths.filter(Files::isRegularFile).count();
    } catch (IOException e) {
      LOGGER.error(Arrays.toString(e.getStackTrace()));
    }

    LOGGER.info("Total number of files to process: " + totalFiles);
    AtomicInteger fileCounter = new AtomicInteger(0);
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      paths.filter(Files::isRegularFile).forEach(path -> {
        int currentFileCounter = fileCounter.incrementAndGet();
        LOGGER.info("Processing file " + currentFileCounter + ": " + path.getFileName());

        Document document;
        switch (fileType) {
          case Constants.FILE_TYPE_CRAWL:
            document = loadDocument(path.toString(), new TextDocumentParser());
            DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_CRAWL, path.getFileName().toString());
            embeddingStoreIngestor.ingest(document);
            break;
          case Constants.FILE_TYPE_TEXT:
            document = loadDocument(path.toString(), new TextDocumentParser());
            DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_TEXT, path.getFileName().toString());
            embeddingStoreIngestor.ingest(document);
            break;
          case Constants.FILE_TYPE_ANY:
            document = loadDocument(path.toString(), new ApacheTikaDocumentParser());
            DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_ANY, path.getFileName().toString());
            embeddingStoreIngestor.ingest(document);
            break;
          default:
            throw new IllegalArgumentException("Unsupported File Type: " + fileType);
        }
      });
    } catch (IOException e) {
      LOGGER.error(Arrays.toString(e.getStackTrace()));
    }
    LOGGER.info("Processing complete");
    return createFolderIngestionStatusObject(totalFiles, fileType);
  }

  public JSONObject readAndIngestFile(String filePath, String fileType) {

    Path path = Paths.get(filePath);

    Document document;
    switch (fileType) {
      case Constants.FILE_TYPE_CRAWL:
        document = loadDocument(path.toString(), new TextDocumentParser());
        DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_CRAWL, path.getFileName().toString());
        break;
      case Constants.FILE_TYPE_TEXT:
        document = loadDocument(path.toString(), new TextDocumentParser());
        DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_TEXT, Utils.getFileNameFromPath(filePath));
        break;
      case Constants.FILE_TYPE_ANY:
        document = loadDocument(path.toString(), new ApacheTikaDocumentParser());
        DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_ANY, Utils.getFileNameFromPath(filePath));
        break;
      case Constants.FILE_TYPE_URL:
        document = loadUrlDocument(filePath);
        break;
      default:
        throw new IllegalArgumentException("Unsupported File Type: " + fileType);
    }
    embeddingStoreIngestor.ingest(document);
    return createFileIngestionStatusObject(fileType, filePath);
  }

  private Document loadUrlDocument(String contextPath) {

    Document document;
    try {
      URL url = new URL(contextPath);
      Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
      HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
      document = transformer.transform(htmlDocument);
      document.metadata().put(Constants.METADATA_KEY_URL, contextPath);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + contextPath, e);
    }
    return document;
  }
}
