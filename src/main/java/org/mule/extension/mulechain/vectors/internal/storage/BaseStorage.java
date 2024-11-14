package org.mule.extension.mulechain.vectors.internal.storage;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.storage.azureblob.AzureBlobStorage;
import org.mule.extension.mulechain.vectors.internal.storage.local.LocalStorage;
import org.mule.extension.mulechain.vectors.internal.storage.s3.AWSS3Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseStorage {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseStorage.class);

  protected String storeName;
  protected Configuration configuration;
  protected EmbeddingStoreIngestor embeddingStoreIngestor;

  public BaseStorage(Configuration configuration, String storeName, EmbeddingStoreIngestor embeddingStoreIngestor) {

    this.storeName = storeName;
    this.configuration = configuration;
    this.embeddingStoreIngestor = embeddingStoreIngestor;
  }

  public JSONObject readAndIngestAllFiles(String contextPath, String fileType) {

    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  public JSONObject readAndIngestFile(String contextPath, String fileType) {

    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  /**
   * Creates a JSONObject representing the ingestion status.
   *
   * @param fileType the type of the ingested file.
   * @param contextPath the path of the ingested file or folder.
   * @return a JSONObject containing ingestion status metadata.
   */
  protected JSONObject createFileIngestionStatusObject(String fileType, String contextPath) {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType);
    jsonObject.put("filePath", contextPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  /**
   * Creates a JSONObject representing the ingestion status of a folder or set of files.
   *
   * @param totalFiles the total number of files processed.
   * @param contextPath the path of the processed folder.
   * @return a JSONObject containing the ingestion status with file count, folder path, store name, and status.
   */
  protected JSONObject createFolderIngestionStatusObject(Long totalFiles, String contextPath) {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("filesCount", totalFiles);
    jsonObject.put("folderPath", contextPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");
    return jsonObject;
  }

  public static BaseStorage.Builder builder() {

    return new BaseStorage.Builder();
  }

  public static class Builder {

    private String storeName;
    private Configuration configuration;
    private String storageType;
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    public Builder() {

    }

    public BaseStorage.Builder storeName(String storeName) {
      this.storeName = storeName;
      return this;
    }

    public BaseStorage.Builder configuration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public BaseStorage.Builder storageType(String storageType) {
      this.storageType = storageType;
      return this;
    }

    public BaseStorage.Builder embeddingStoreIngestor(EmbeddingStoreIngestor embeddingStoreIngestor) {
      this.embeddingStoreIngestor = embeddingStoreIngestor;
      return this;
    }

    public BaseStorage build() {

      BaseStorage baseStorage;

      LOGGER.debug("Storage Type: " + storageType);
      switch (storageType) {

        case Constants.STORAGE_TYPE_LOCAL:

          baseStorage = new LocalStorage(configuration, storeName, embeddingStoreIngestor);
          break;
        case Constants.STORAGE_TYPE_S3:

          baseStorage = new AWSS3Storage(configuration, storeName, embeddingStoreIngestor);
          break;

        case Constants.STORAGE_TYPE_AZURE_BLOB:

          baseStorage = new AzureBlobStorage(configuration, storeName, embeddingStoreIngestor);
          break;

        default:
          //throw new IllegalOperationException("Unsupported Vector Store: " + configuration.getVectorStore());
          baseStorage = null;
      }
      return baseStorage;
    }
  }
}
