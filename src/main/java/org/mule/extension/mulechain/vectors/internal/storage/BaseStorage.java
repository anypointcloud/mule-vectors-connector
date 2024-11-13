package org.mule.extension.mulechain.vectors.internal.storage;

import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.FileTypeParameters;

public abstract class BaseStorage {

  protected EmbeddingStoreIngestor ingestor;

  public BaseStorage(EmbeddingStoreIngestor ingestor) {
    this.ingestor = ingestor;
  }

  public abstract long readAllFiles(String contextPath, FileTypeParameters fileType);

  public abstract long readFile(String contextPath, FileTypeParameters fileType);
}
