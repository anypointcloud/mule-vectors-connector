package org.mule.extension.mulechain.vectors.internal.storage;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.storage.azureblob.AzureBlobStorage;
import org.mule.extension.mulechain.vectors.internal.storage.local.LocalStorage;
import org.mule.extension.mulechain.vectors.internal.storage.s3.AWSS3Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public abstract class BaseStorage implements Iterator<Document> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseStorage.class);

  protected Configuration configuration;
  protected String contextPath;
  protected String fileType;
  protected DocumentParser documentParser;

  public BaseStorage(Configuration configuration, String contextPath, String fileType) {

    this.configuration = configuration;
    this.contextPath = contextPath;
    this.fileType = fileType;
    this.documentParser = getDocumentParser(fileType);
  }

  @Override
  public boolean hasNext() {
    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  @Override
  public Document next() {
    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  public Document getSingleDocument() {
    throw new UnsupportedOperationException("This method should be overridden by subclasses");
  }

  protected DocumentParser getDocumentParser(String fileType) {

    DocumentParser documentParser = null;
    switch (fileType){

      case Constants.FILE_TYPE_TEXT:
      case Constants.FILE_TYPE_CRAWL:
        documentParser = new TextDocumentParser();
        break;
      case Constants.FILE_TYPE_ANY:
        documentParser = new ApacheTikaDocumentParser();
        break;
      default:
        throw new IllegalArgumentException("Unsupported File Type: " + fileType);
    }
    return documentParser;
  }

  public static BaseStorage.Builder builder() {

    return new BaseStorage.Builder();
  }

  public static class Builder {

    private Configuration configuration;
    private String storageType;
    private String contextPath;
    private String fileType;

    public Builder() {

    }

    public BaseStorage.Builder configuration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public BaseStorage.Builder storageType(String storageType) {
      this.storageType = storageType;
      return this;
    }

    public BaseStorage.Builder contextPath(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    public BaseStorage.Builder fileType(String fileType) {
      this.fileType = fileType;
      return this;
    }

    public BaseStorage build() {

      BaseStorage baseStorage;

      LOGGER.debug("Storage Type: " + storageType);
      switch (storageType) {

        case Constants.STORAGE_TYPE_LOCAL:

          baseStorage = new LocalStorage(configuration, contextPath, fileType);
          break;
        case Constants.STORAGE_TYPE_S3:

          baseStorage = new AWSS3Storage(configuration, contextPath, fileType);
          break;

        case Constants.STORAGE_TYPE_AZURE_BLOB:

          baseStorage = new AzureBlobStorage(configuration, contextPath, fileType);
          break;

        default:
          //throw new IllegalOperationException("Unsupported Vector Store: " + configuration.getVectorStore());
          baseStorage = null;
      }
      return baseStorage;
    }
  }
}
