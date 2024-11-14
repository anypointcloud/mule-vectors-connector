package org.mule.extension.mulechain.vectors.internal.store.pgvector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

/**
 * Represents a store for vector data using PostgreSQL with PGVector extension.
 * This class is responsible for interacting with a PostgreSQL database to store and retrieve vector metadata.
 */
public class PGVectorStore extends BaseStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(PGVectorStore.class);

  private String userName;
  private String password;
  private String host;
  private int port;
  private String database;

  /**
   * Constructs a PGVectorVectorStore instance using configuration and query parameters.
   *
   * @param storeName The name of the store.
   * @param configuration The configuration for connecting to the store.
   * @param queryParams Parameters related to query configurations.
   */
  public PGVectorStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

    super(storeName, configuration, queryParams, dimension);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_PGVECTOR);
    this.host = vectorStoreConfig.getString("POSTGRES_HOST");
    this.port = vectorStoreConfig.getInt("POSTGRES_PORT");
    this.database = vectorStoreConfig.getString("POSTGRES_DATABASE");
    this.userName = vectorStoreConfig.getString("POSTGRES_USER");
    this.password = vectorStoreConfig.getString("POSTGRES_PASSWORD");
  }

  public EmbeddingStore<TextSegment> buildEmbeddingStore() {

    return PgVectorEmbeddingStore.builder()
        .host(host)
        .port(port)
        .database(database)
        .user(userName)
        .password(password)
        .table(storeName)
        .dimension(dimension)
        .build();
  }

  /**
   * Lists the sources stored in the PostgreSQL database.
   *
   * @return A {@link JSONObject} containing the sources and their metadata.
   */
  public JSONObject listSources() {

    HashMap<String, JSONObject> sourceObjectMap = new HashMap<>();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(JSON_KEY_STORE_NAME, storeName);

    try (PgVectorMetadataIterator iterator = new PgVectorMetadataIterator(userName, password, host, port, database, storeName, (int)queryParams.embeddingPageSize())) {
      while (iterator.hasNext()) {

        JSONObject metadataObject = new JSONObject(iterator.next());
        JSONObject sourceObject = getSourceObject(metadataObject);
        addOrUpdateSourceObjectIntoSourceObjectMap(sourceObjectMap, sourceObject);
      }
    } catch (SQLException e) {
      LOGGER.error("Error while listing sources", e);
    }

    jsonObject.put(JSON_KEY_SOURCES, JsonUtils.jsonObjectCollectionToJsonArray(sourceObjectMap.values()));
    jsonObject.put(JSON_KEY_SOURCE_COUNT, sourceObjectMap.size());

    return jsonObject;
  }

  /**
   * Iterator to handle metadata pagination from the PostgreSQL database.
   */
  private class PgVectorMetadataIterator implements Iterator<String>, AutoCloseable {

    private int offset = 0; // Current offset for pagination
    private ResultSet resultSet;
    private PreparedStatement pstmt;
    private Connection connection;
    private String table;
    int pageSize;

    /**
     * Constructs a PgVectorMetadataIterator for fetching metadata from the database in pages.
     *
     * @param userName The username for database access.
     * @param password The password for database access.
     * @param host The PostgreSQL host.
     * @param port The PostgreSQL port.
     * @param database The name of the database.
     * @param table The table to fetch metadata from.
     * @param pageSize The number of rows per page for pagination.
     * @throws SQLException If a database error occurs.
     */
    private PgVectorMetadataIterator(String userName, String password, String host, int port, String database, String table, int pageSize) throws SQLException {

      // Initialize the connection and the first page of data
      PGSimpleDataSource dataSource = new PGSimpleDataSource();
      dataSource.setServerNames(new String[]{host});
      dataSource.setPortNumbers(new int[]{port});
      dataSource.setDatabaseName(database);
      dataSource.setUser(userName);
      dataSource.setPassword(password);

      connection = dataSource.getConnection();

      this.table = table;
      this.pageSize = pageSize;

      fetchNextPage();
    }

    /**
     * Fetches the next page of metadata from the database.
     *
     * @throws SQLException If a database error occurs.
     */
    private void fetchNextPage() throws SQLException {
      if (pstmt != null) {
        pstmt.close();
      }

      String query = "SELECT " + Constants.STORE_SCHEMA_METADATA_FIELD_NAME  + " FROM " + table + " LIMIT ? OFFSET ?";
      pstmt = connection.prepareStatement(query);
      pstmt.setInt(1, this.pageSize);
      pstmt.setInt(2, offset);
      resultSet = pstmt.executeQuery();
      offset += pageSize;
    }

    /**
     * Checks if there are more elements in the result set.
     *
     * @return {@code true} if there are more elements, {@code false} otherwise.
     */
    @Override
    public boolean hasNext() {
      try {
        // Check if the resultSet has more rows or fetch the next page
        if (resultSet != null && resultSet.next()) {
          return true;
        } else {
          fetchNextPage(); // Fetch the next page if the current page is exhausted
          return resultSet != null && resultSet.next();
        }
      } catch (SQLException e) {
        LOGGER.error("Error checking for next element", e);
        return false;
      }
    }

    /**
     * Returns the next metadata element in the result set.
     *
     * @return The next metadata element as a {@code String}.
     * @throws NoSuchElementException If no more elements are available.
     */
    @Override
    public String next() {
      try {
        if (resultSet == null) {
          throw new NoSuchElementException("No more elements available");
        }
        return resultSet.getString(Constants.STORE_SCHEMA_METADATA_FIELD_NAME);
      } catch (SQLException e) {
        LOGGER.error("Error retrieving next element", e);
        throw new NoSuchElementException("Error retrieving next element");
      }
    }

    /**
     * Closes the iterator and releases the resources.
     */
    public void close() {
      try {
        if (resultSet != null)
          resultSet.close();
        if (pstmt != null)
          pstmt.close();
        if (connection != null)
          connection.close();
      } catch (SQLException e) {
        LOGGER.error("Error closing resources", e);
      }
    }
  }
}
