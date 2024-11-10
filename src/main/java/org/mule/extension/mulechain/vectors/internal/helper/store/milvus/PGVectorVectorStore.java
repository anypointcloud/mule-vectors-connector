package org.mule.extension.mulechain.vectors.internal.helper.store.milvus;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.EmbeddingModelNameParameters;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.helper.store.VectorStore;
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

public class PGVectorVectorStore extends VectorStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(PGVectorVectorStore.class);

  private String userName;
  private String password;
  private String host;
  private int port;
  private String database;
  private int pageSize;

  public PGVectorVectorStore(String storeName, Configuration configuration, QueryParameters queryParams, EmbeddingModelNameParameters modelParams) {

    super(storeName, configuration, queryParams, modelParams);

    JSONObject config = readConfigFile(configuration.getConfigFilePath());
    JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_PGVECTOR);
    this.host = vectorStoreConfig.getString("POSTGRES_HOST");
    this.port = vectorStoreConfig.getInt("POSTGRES_PORT");
    this.database = vectorStoreConfig.getString("POSTGRES_DATABASE");
    this.userName = vectorStoreConfig.getString("POSTGRES_USER");
    this.password = vectorStoreConfig.getString("POSTGRES_PASSWORD");
  }

  public JSONObject listSources() {

    HashMap<String, JSONObject> sourcesJSONObjectHashMap = new HashMap<String, JSONObject>();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);

    try (PgVectorMetadataIterator iterator = new PgVectorMetadataIterator(userName, password, host, port, database, storeName, (int)queryParams.embeddingPageSize())) {
      while (iterator.hasNext()) {

        JSONObject metadataObject = new JSONObject(iterator.next());

        String index = metadataObject.has(Constants.METADATA_KEY_INDEX) ? metadataObject.getString(Constants.METADATA_KEY_INDEX) : null;
        JSONObject sourceObject = getSourceObject(metadataObject);

        String sourceUniqueKey = getSourceUniqueKey(sourceObject);

        // Add sourceObject to sources only if it has at least one key-value pair and it's possible to generate a key
        if (!sourceObject.isEmpty() && sourceUniqueKey != null && !sourceUniqueKey.isEmpty()) {
          // Overwrite sourceObject if current one has a greater index (greatest index represents the number of segments)
          if(sourcesJSONObjectHashMap.containsKey(sourceUniqueKey)){
            // Get current index
            int currentSegmentCount = Integer.parseInt(index) + 1;
            // Get previously stored index
            int storedSegmentCount = (int) sourcesJSONObjectHashMap.get(sourceUniqueKey).get("segmentCount");
            // Check if object need to be updated
            if(currentSegmentCount > storedSegmentCount) {
              sourcesJSONObjectHashMap.put(sourceUniqueKey, sourceObject);
            }
          } else {
            sourcesJSONObjectHashMap.put(sourceUniqueKey, sourceObject);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    jsonObject.put("sources", JsonUtils.jsonObjectCollectionToJsonArray(sourcesJSONObjectHashMap.values()));
    jsonObject.put("sourceCount", sourcesJSONObjectHashMap.size());

    return jsonObject;
  }

  public class PgVectorMetadataIterator implements Iterator<String>, AutoCloseable {

    private int offset = 0; // Current offset for pagination
    private ResultSet resultSet;
    private PreparedStatement pstmt;
    private Connection connection;
    private String table;
    int pageSize;

    public PgVectorMetadataIterator(String userName, String password, String host, int port, String database, String table, int pageSize) throws SQLException {

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

    // Fetch the next page of data
    private void fetchNextPage() throws SQLException {
      if (pstmt != null) {
        pstmt.close();
      }

      String query = "SELECT metadata FROM " + table + " LIMIT ? OFFSET ?";
      pstmt = connection.prepareStatement(query);
      pstmt.setInt(1, this.pageSize);
      pstmt.setInt(2, offset);
      resultSet = pstmt.executeQuery();
      offset += pageSize;
    }

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
        e.printStackTrace();
        return false;
      }
    }

    @Override
    public String next() {
      try {
        if (resultSet == null) {
          throw new NoSuchElementException("No more elements available");
        }
        return resultSet.getString("metadata");
      } catch (SQLException e) {
        e.printStackTrace();
        throw new NoSuchElementException("Error retrieving next element");
      }
    }

    // Clean up resources after iteration
    public void close() {
      try {
        if (resultSet != null)
          resultSet.close();
        if (pstmt != null)
          pstmt.close();
        if (connection != null)
          connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
