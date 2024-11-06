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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class EmbeddingStoreIngestorHelper {

  public static JSONObject ingestFromLocalFolder(String folderPath, dev.langchain4j.store.embedding.EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType) {
    long totalFiles = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      totalFiles = paths.filter(Files::isRegularFile).count();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Total number of files to process: " + totalFiles);
    AtomicInteger fileCounter = new AtomicInteger(0);
    try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
      paths.filter(Files::isRegularFile).forEach(file -> {
        int currentFileCounter = fileCounter.incrementAndGet();
        System.out.println("Processing file " + currentFileCounter + ": " + file.getFileName());
        Document document = null;
        switch (fileType.getFileType()) {
          case Constants.FILE_TYPE_CRAWL:
            document = loadDocument(file.toString(), new TextDocumentParser());
            DocumentUtils.addMetadataToDocument(Paths.get(file.toString()), document);
            ingestor.ingest(document);
            break;
          case Constants.FILE_TYPE_TEXT:
            document = loadDocument(file.toString(), new TextDocumentParser());
            System.out.println("File: " + file.toString());
            document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
            document.metadata().add(Constants.METADATA_KEY_FILE_NAME, file.getFileName());
            document.metadata().add(Constants.METADATA_KEY_FULL_PATH, folderPath + file.getFileName());
            document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
            ingestor.ingest(document);
            break;
          case Constants.FILE_TYPE_ANY:
            document = loadDocument(file.toString(), new ApacheTikaDocumentParser());
            System.out.println("File: " + file.toString());
            document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_ANY);
            document.metadata().add(Constants.METADATA_KEY_FILE_NAME, file.getFileName());
            document.metadata().add(Constants.METADATA_KEY_FULL_PATH, folderPath + file.getFileName());
            document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
            ingestor.ingest(document);
            break;
          default:
            throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Processing complete ");
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("filesCount", totalFiles);
    jsonObject.put("folderPath", folderPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }


  public static JSONObject ingestFromLocalFile(String contextPath, EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType) {

    System.out.println("file Type: " + fileType.getFileType());

    Document document = null;
    Path filePath;
    String fileName;

    switch (fileType.getFileType()) {
      case Constants.FILE_TYPE_CRAWL:
        filePath = Paths.get(contextPath.toString());
        fileName = getFileNameFromPath(contextPath);

        document = loadDocument(filePath.toString(), new TextDocumentParser());
        DocumentUtils.addMetadataToDocument(filePath, document);
        ingestor.ingest(document);

        break;
      case Constants.FILE_TYPE_TEXT:
        filePath = Paths.get(contextPath.toString());
        fileName = getFileNameFromPath(contextPath);
        document = loadDocument(filePath.toString(), new TextDocumentParser());
        document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_TEXT);
        document.metadata().add(Constants.METADATA_KEY_FILE_NAME, fileName);
        document.metadata().add(Constants.METADATA_KEY_FULL_PATH, contextPath);
        document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
        ingestor.ingest(document);


        break;
      case Constants.FILE_TYPE_ANY:
        filePath = Paths.get(contextPath.toString());
        fileName = getFileNameFromPath(contextPath);
        document = loadDocument(filePath.toString(), new ApacheTikaDocumentParser());
        document.metadata().add(Constants.METADATA_KEY_FILE_TYPE, Constants.FILE_TYPE_ANY);
        document.metadata().add(Constants.METADATA_KEY_FILE_NAME, fileName);
        document.metadata().add(Constants.METADATA_KEY_FULL_PATH, contextPath);
        document.metadata().add(Constants.METADATA_KEY_INGESTION_DATETIME, Utils.getCurrentISO8601Timestamp());
        ingestor.ingest(document);

        break;
      case Constants.FILE_TYPE_URL:
        System.out.println("Context Path: " + contextPath);

        URL url = null;
        try {
          url = new URL(contextPath);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }

        Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
        HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
        document = transformer.transform(htmlDocument);
        document.metadata().add(Constants.METADATA_KEY_URL, contextPath);
        ingestor.ingest(document);

        break;
      default:
        throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
    }



    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType.getFileType());
    jsonObject.put("filePath", contextPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");

    return jsonObject;
  }

  public static JSONObject ingestFromS3Folder(String folderPath, dev.langchain4j.store.embedding.EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String awsKey, String awsSecret, String awsRegion, String s3Bucket)
  {
    S3FileReader s3FileReader = new S3FileReader(s3Bucket, awsKey, awsSecret, awsRegion);
    long totalFiles = s3FileReader.readAllFiles(folderPath, ingestor, fileType);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("filesCount", totalFiles);
    jsonObject.put("folderPath", folderPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  public static JSONObject ingestFromS3File(String folderPath, dev.langchain4j.store.embedding.EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String awsKey, String awsSecret, String awsRegion, String s3Bucket)
  {
    S3FileReader s3FileReader = new S3FileReader(s3Bucket, awsKey, awsSecret, awsRegion);
    s3FileReader.readFile(folderPath, fileType, ingestor);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType.getFileType());
    jsonObject.put("folderPath", folderPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  public static JSONObject ingestFromAZContainer(String containerName, dev.langchain4j.store.embedding.EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String azureName, String azureKey)
  {
    AzureFileReader azFileReader = new AzureFileReader(azureName, azureKey);
    azFileReader.readAllFiles(containerName, ingestor, fileType);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType.getFileType());
    jsonObject.put("folderPath", containerName);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  public static JSONObject ingestFromAZFile(String containerName, String blobName, dev.langchain4j.store.embedding.EmbeddingStoreIngestor ingestor, String storeName, FileTypeParameters fileType, String azureName, String azureKey)
  {
    AzureFileReader azFileReader = new AzureFileReader(azureName, azureKey);
    azFileReader.readFile(containerName, blobName, fileType, ingestor);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType.getFileType());
    jsonObject.put("folderPath", containerName);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  private static String getFileNameFromPath(String fullPath) {

    File file = new File(fullPath);
    return file.getName();
  }
}
