package org.mule.extension.vectors.internal.connection.store.qdrant;

import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public class QdrantStoreConnection implements BaseStoreConnection {

  private String host;
  private int gprcPort;
  private boolean useTLS;
  private String textSegmentKey;
  private String apiKey;

  public QdrantStoreConnection(String host, int gprcPort, boolean useTLS, String textSegmentKey, String apiKey) {
    this.host = host;
    this.gprcPort = gprcPort;
    this.useTLS = useTLS;
    this.textSegmentKey = textSegmentKey;
    this.apiKey = apiKey;
  }

  public String getHost() {
    return host;
  }

  public int getGprcPort() {
    return gprcPort;
  }

  public boolean isUseTLS() {
    return useTLS;
  }

  public String getTextSegmentKey() {
    return textSegmentKey;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_QDRANT;
  }
}
