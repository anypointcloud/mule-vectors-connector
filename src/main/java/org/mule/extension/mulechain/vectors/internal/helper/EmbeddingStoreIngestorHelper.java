package org.mule.extension.mulechain.vectors.internal.helper;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.transformer.jsoup.HtmlToTextDocumentTransformer;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.FileTypeParameters;
import org.mule.extension.mulechain.vectors.internal.storage.AzureFileReader;
import org.mule.extension.mulechain.vectors.internal.storage.S3FileReader;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * Helper class for ingesting documents into an embedding store from various sources, including local files,
 * S3, and Azure storage containers.
 */
public class EmbeddingStoreIngestorHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingStoreIngestorHelper.class);

  private EmbeddingStoreIngestor ingestor;
  private String storeName;

  /**
   * Constructs a new EmbeddingStoreIngestorHelper.
   *
   * @param ingestor the embedding store ingestor used to store documents.
   * @param storeName the name of the store where documents are being ingested.
   */
  public EmbeddingStoreIngestorHelper(EmbeddingStoreIngestor ingestor, String storeName) {

    this.ingestor = ingestor;
    this.storeName = storeName;
  }

  /**
   * Ingests documents from a local folder into the embedding store.
   *
   * @param folderPath the path of the folder containing documents to ingest.
   * @param fileTypeParameters the type of files to ingest, as defined by FileTypeParameters.
   * @return a JSONObject with ingestion status and metadata.
   */
  public JSONObject ingestFromLocalFolder(String folderPath, FileTypeParameters fileTypeParameters) {

    long totalFiles = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      totalFiles = paths.filter(Files::isRegularFile).count();
    } catch (IOException e) {
      e.printStackTrace();
    }

    LOGGER.info("Total number of files to process: " + totalFiles);
    AtomicInteger fileCounter = new AtomicInteger(0);
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      paths.filter(Files::isRegularFile).forEach(path -> {
        int currentFileCounter = fileCounter.incrementAndGet();
        LOGGER.info("Processing file " + currentFileCounter + ": " + path.getFileName());

        Document document;
        switch (fileTypeParameters.getFileType()) {
          case Constants.FILE_TYPE_CRAWL:
            document = loadDocument(path.toString(), new TextDocumentParser());
            DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_CRAWL, path);
            ingestor.ingest(document);
            break;
          case Constants.FILE_TYPE_TEXT:
            document = loadDocument(path.toString(), new TextDocumentParser());
            DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_TEXT, path.getFileName().toString(), folderPath + path.getFileName());
            ingestor.ingest(document);
            break;
          case Constants.FILE_TYPE_ANY:
            document = loadDocument(path.toString(), new ApacheTikaDocumentParser());
            DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_ANY, path.getFileName().toString(),folderPath + path.getFileName());
            ingestor.ingest(document);
            break;
          default:
            throw new IllegalArgumentException("Unsupported File Type: " + fileTypeParameters.getFileType());
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
    LOGGER.info("Processing complete");

    return createFolderIngestionStatusObject(totalFiles, folderPath);
  }

  /**
   * Ingests a single local file into the embedding store.
   *
   * @param filePath the path of the file to ingest.
   * @param fileTypeParameters the type of file, as defined by FileTypeParameters.
   * @return a JSONObject with ingestion status and metadata.
   */
  public JSONObject ingestFromLocalFile(String filePath, FileTypeParameters fileTypeParameters) {

    Path path = Paths.get(filePath);

    Document document;
    switch (fileTypeParameters.getFileType()) {
      case Constants.FILE_TYPE_CRAWL:
        document = loadDocument(path.toString(), new TextDocumentParser());
        DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_CRAWL, path);
        break;
      case Constants.FILE_TYPE_TEXT:
        document = loadDocument(path.toString(), new TextDocumentParser());
        DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_TEXT, Utils.getFileNameFromPath(filePath), filePath);
        break;
      case Constants.FILE_TYPE_ANY:
        document = loadDocument(path.toString(), new ApacheTikaDocumentParser());
        DocumentUtils.addMetadataToDocument(document, Constants.FILE_TYPE_ANY, Utils.getFileNameFromPath(filePath), filePath);
        break;
      case Constants.FILE_TYPE_URL:
        document = loadUrlDocument(filePath);
        break;
      default:
        throw new IllegalArgumentException("Unsupported File Type: " + fileTypeParameters.getFileType());
    }
    ingestor.ingest(document);
    return createFileIngestionStatusObject(fileTypeParameters.getFileType(), filePath);
  }

  /**
   * Ingests documents from an S3 folder into the embedding store.
   *
   * @param folderPath the path of the folder in S3.
   * @param fileTypeParameters the type of files to ingest, as defined by FileTypeParameters.
   * @param awsKey AWS access key.
   * @param awsSecret AWS secret key.
   * @param awsRegion AWS region.
   * @param s3Bucket the name of the S3 bucket.
   * @return a JSONObject with ingestion status and metadata.
   */
  public JSONObject ingestFromS3Folder(String folderPath, FileTypeParameters fileTypeParameters, String awsKey, String awsSecret, String awsRegion, String s3Bucket) {

    S3FileReader s3FileReader = new S3FileReader(s3Bucket, awsKey, awsSecret, awsRegion);
    long totalFiles = s3FileReader.readAllFiles(folderPath, ingestor, fileTypeParameters);
    return createFolderIngestionStatusObject(totalFiles, folderPath);
  }

  /**
   * Ingests a single file from an S3 bucket into the embedding store.
   *
   * @param filePath the path of the file in the S3 bucket.
   * @param fileTypeParameters the type of file to ingest, as defined by FileTypeParameters.
   * @param awsKey AWS access key.
   * @param awsSecret AWS secret key.
   * @param awsRegion AWS region.
   * @param s3Bucket the name of the S3 bucket.
   * @return a JSONObject with ingestion status and metadata.
   */
  public JSONObject ingestFromS3File(String filePath, FileTypeParameters fileTypeParameters, String awsKey, String awsSecret, String awsRegion, String s3Bucket) {

    S3FileReader s3FileReader = new S3FileReader(s3Bucket, awsKey, awsSecret, awsRegion);
    s3FileReader.readFile(filePath, fileTypeParameters, ingestor);
    return createFileIngestionStatusObject(fileTypeParameters.getFileType(), filePath);
  }

  /**
   * Ingests documents from an Azure storage container into the embedding store.
   *
   * @param containerName the name of the Azure container.
   * @param fileType the type of files to ingest, as defined by FileTypeParameters.
   * @param azureName the Azure storage account name.
   * @param azureKey the Azure storage account key.
   * @return a JSONObject with ingestion status and metadata.
   */
  public JSONObject ingestFromAZContainer(String containerName, FileTypeParameters fileType, String azureName, String azureKey) {

    AzureFileReader azFileReader = new AzureFileReader(azureName, azureKey);
    long totalFiles = azFileReader.readAllFiles(containerName, ingestor, fileType);
    return createFolderIngestionStatusObject(totalFiles, containerName);
  }

  /**
   * Ingests a single file from an Azure storage container into the embedding store.
   *
   * @param containerName the name of the Azure container.
   * @param blobName the name of the file in the Azure container.
   * @param fileType the type of file to ingest, as defined by FileTypeParameters.
   * @param azureName the Azure storage account name.
   * @param azureKey the Azure storage account key.
   * @return a JSONObject with ingestion status and metadata.
   */
  public JSONObject ingestFromAZFile(String containerName, String blobName, FileTypeParameters fileType, String azureName, String azureKey) {

    AzureFileReader azFileReader = new AzureFileReader(azureName, azureKey);
    azFileReader.readFile(containerName, blobName, fileType, ingestor);
    return createFileIngestionStatusObject(fileType.getFileType(), containerName);
  }

  /**
   * Loads and transforms a document from a URL into a text document.
   *
   * @param contextPath the URL to load.
   * @return the transformed document.
   */
  private Document loadUrlDocument(String contextPath) {

    Document document;
    try {
      URL url = new URL(contextPath);
      Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
      HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
      document = transformer.transform(htmlDocument);
      document.metadata().add(Constants.METADATA_KEY_URL, contextPath);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + contextPath, e);
    }
    return document;
  }

  /**
   * Creates a JSONObject representing the ingestion status.
   *
   * @param fileType the type of the ingested file.
   * @param folderPath the path of the ingested file or folder.
   * @return a JSONObject containing ingestion status metadata.
   */
  private JSONObject createFileIngestionStatusObject(String fileType, String folderPath) {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType);
    jsonObject.put("filePath", folderPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  /**
   * Creates a JSONObject representing the ingestion status of a folder or set of files.
   *
   * @param totalFiles the total number of files processed.
   * @param folderPath the path of the processed folder.
   * @return a JSONObject containing the ingestion status with file count, folder path, store name, and status.
   */
  private JSONObject createFolderIngestionStatusObject(Long totalFiles, String folderPath) {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("filesCount", totalFiles);
    jsonObject.put("folderPath", folderPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

}
