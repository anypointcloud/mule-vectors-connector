package org.mule.extension.vectors.internal.connection.store.qdrant;

import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import org.mule.extension.vectors.internal.connection.store.BaseStoreConnection;
import org.mule.extension.vectors.internal.connection.store.elasticsearch.ElasticsearchStoreConnection;
import org.mule.extension.vectors.internal.constant.Constants;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class QdrantStoreConnection implements BaseStoreConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(QdrantStoreConnection.class);

  private final String host;
  private final int gprcPort;
  private final boolean useTLS;
  private final String textSegmentKey;
  private final String apiKey;

  private QdrantClient client;

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

  public QdrantClient getClient() {
    return client;
  }

  @Override
  public String getVectorStore() {
    return Constants.VECTOR_STORE_QDRANT;
  }

  @Override
  public void connect() throws ConnectionException {

    try {

      this.client = new QdrantClient(
          QdrantGrpcClient.newBuilder(host, gprcPort, useTLS)
              .withApiKey(apiKey)
              .build()
      );

      doHealthCheck();

    } catch (ConnectionException e) {

      throw e;
    } catch (Exception e) {

      throw new ConnectionException("Impossible to connect to Qdrant.", e);
    }
  }

  @Override
  public void disconnect() {

    this.client.close();
  }

  @Override
  public boolean isValid() {

    try {

      doHealthCheck();
      return true;

    } catch (Exception e) {

      LOGGER.error("Failed to validate connection to Qdrant.", e);
      return false;
    }

  }

  private void doHealthCheck() throws Exception {

    // Assuming you have a method `healthCheckAsync` that returns ListenableFuture
    ListenableFuture<QdrantOuterClass.HealthCheckReply> healthCheckFuture = this.client.healthCheckAsync();

    // Make it synchronous by calling get()
    QdrantOuterClass.HealthCheckReply healthCheckReply = healthCheckFuture.get();
  }
}
