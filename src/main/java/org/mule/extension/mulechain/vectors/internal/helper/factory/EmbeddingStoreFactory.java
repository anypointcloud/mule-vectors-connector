package org.mule.extension.mulechain.vectors.internal.helper.factory;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class EmbeddingStoreFactory {

    public static EmbeddingStore<TextSegment> createStore(Configuration configuration, String indexName, Integer dimension) {
        EmbeddingStore<TextSegment> store = null;
        JSONObject config = readConfigFile(configuration.getConfigFilePath());
        JSONObject vectorType;
        String userName;
        String password;
        String vectorHost;
        Integer vectorPort;
        String vectorDatabase;
        String vectorApiKey;

        String vectorUrl;
        switch (configuration.getVectorStore()) {
            case Constants.VECTOR_STORE_AI_SEARCH:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_AI_SEARCH);
                vectorApiKey = vectorType.getString("AI_SEARCH_KEY");
                vectorUrl = vectorType.getString("AI_SEARCH_URL");
                store = createAISearchStore(vectorUrl, vectorApiKey, indexName, dimension);
                break;

            case Constants.VECTOR_STORE_CHROMA:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_CHROMA);
                vectorUrl = vectorType.getString("CHROMA_URL");
                store = createChromaStore(vectorUrl, indexName);
                break;

            case Constants.VECTOR_STORE_MILVUS:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_MILVUS);
                vectorUrl = vectorType.getString("MILVUS_URL");
                store = createMilvusStore(vectorUrl, indexName, dimension);
                break;

            case Constants.VECTOR_STORE_PINECONE:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_PINECONE);
                vectorApiKey = vectorType.getString("PINECONE_APIKEY");
                String vectorCloud = vectorType.getString("PINECONE_SERVERLESS_CLOUD");
                String vectorCloudRegion = vectorType.getString("PINECONE_SERVERLESS_REGION");
                store = createPineconeStore(vectorApiKey, vectorCloud, vectorCloudRegion, indexName, dimension);
                break;

            case Constants.VECTOR_STORE_ELASTICSEARCH:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_ELASTICSEARCH);
                vectorUrl = vectorType.getString("ELASTICSEARCH_URL");
                userName = vectorType.getString("ELASTICSEARCH_USER");
                password = vectorType.getString("ELASTICSEARCH_PASSWORD");
                store = createElasticStore(vectorUrl, userName, password, indexName, dimension);
                break;

            case Constants.VECTOR_STORE_OPENSEARCH:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_OPENSEARCH);
                vectorUrl = vectorType.getString("OPENSEARCH_URL");
                userName = vectorType.getString("OPENSEARCH_USER");
                password = vectorType.getString("OPENSEARCH_PASSWORD");
                store = createOpenSrchStore(vectorUrl, userName, password, indexName, dimension);
                break;

            case Constants.VECTOR_STORE_PGVECTOR:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_PGVECTOR);
                vectorHost = vectorType.getString("POSTGRES_HOST");
                vectorPort = vectorType.getInt("POSTGRES_PORT");
                vectorDatabase = vectorType.getString("POSTGRES_DATABASE");
                userName = vectorType.getString("POSTGRES_USER");
                password = vectorType.getString("POSTGRES_PASSWORD");
                store = createPGVectorStore(vectorHost, vectorPort, vectorDatabase, userName, password, indexName, dimension);
                break;

            case Constants.VECTOR_STORE_WEAVIATE:
                vectorType = config.getJSONObject(Constants.VECTOR_STORE_WEAVIATE);
                vectorHost = vectorType.getString("WEAVIATE_HOST");
                String vectorProtocol = vectorType.getString("WEAVIATE_PROTOCOL");
                vectorApiKey = vectorType.getString("WEAVIATE_APIKEY");
                String weaviateIdex = indexName.substring(0, 1).toUpperCase() + indexName.substring(1);
                store = createWeaviateStore(vectorProtocol, vectorHost, vectorApiKey, weaviateIdex);
                break;
            default:
                throw new IllegalArgumentException("Unsupported VectorDB type: " + configuration.getEmbeddingModelService());
        }

        return store;
    }

    private static EmbeddingStore<TextSegment> createAISearchStore(String baseUrl, String apiKey, String collectionName, Integer dimension) {
        return AzureAiSearchEmbeddingStore.builder()
                .endpoint(baseUrl)
                .apiKey(apiKey)
                .indexName(collectionName)
                .dimensions(dimension)
                .build();

    }

    private static EmbeddingStore<TextSegment> createChromaStore(String baseUrl, String collectionName) {
        return ChromaEmbeddingStore.builder()
                .baseUrl(baseUrl)
                .collectionName(collectionName)
                .build();
    }

    private static EmbeddingStore<TextSegment> createMilvusStore(String baseUrl, String collectionName, Integer dimension) {
        return MilvusEmbeddingStore.builder()
                .uri(baseUrl)
                .collectionName(collectionName)
                .dimension(dimension)
                .build();
    }

    private static EmbeddingStore<TextSegment> createElasticStore(String baseUrl, String userName, String password, String collectionName, Integer dimension) {
        return ElasticsearchEmbeddingStore.builder()
                .serverUrl(baseUrl)
                .userName(userName)
                .password(password)
                .indexName(collectionName)
                .dimension(dimension)
                .build();
    }

    private static EmbeddingStore<TextSegment> createOpenSrchStore(String baseUrl, String userName, String password, String collectionName, Integer dimension) {
        return OpenSearchEmbeddingStore.builder()
                .serverUrl(baseUrl)
                .userName(userName)
                .password(password)
                .indexName(collectionName)
                .build();
    }

    private static EmbeddingStore<TextSegment> createPineconeStore(String apiKey, String cloudProvider, String cloudRegion, String collectionName, Integer dimension) {
        return PineconeEmbeddingStore.builder()
                .apiKey(apiKey)
                .index(collectionName)
                .nameSpace("ns0mc_" + collectionName)
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud(cloudProvider)
                        .region(cloudRegion)
                        .dimension(dimension)
                        .build())
                .build();
    }

    private static EmbeddingStore<TextSegment> createPGVectorStore(String host, Integer port, String database, String userName, String password, String collectionName, Integer dimension) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(userName)
                .password(password)
                .table(collectionName)
                .dimension(dimension)
                .build();
    }

    private static EmbeddingStore<TextSegment> createWeaviateStore(String protocol, String host, String apiKey, String collectionName) {
        return WeaviateEmbeddingStore.builder()
                .scheme(protocol)
                .host(host)
                // "Default" class is used if not specified. Must start from an uppercase letter!
                .objectClass(collectionName)
                // If true (default), then WeaviateEmbeddingStore will generate a hashed ID based on provided
                // text segment, which avoids duplicated entries in DB. If false, then random ID will be generated.
                .avoidDups(true)
                // Consistency level: ONE, QUORUM (default) or ALL.
                .consistencyLevel("ALL")
                .apiKey(apiKey)
                .build();
    }
}
