package org.mule.extension.mulechain.vectors.internal.store.qdrant;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constant.Constants;
import org.mule.extension.mulechain.vectors.internal.helper.parameter.QueryParameters;
import org.mule.extension.mulechain.vectors.internal.store.BaseStore;
import org.mule.extension.mulechain.vectors.internal.util.JsonUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class QdrantStore extends BaseStore {

    private final QdrantClient client;
    private final String payloadTextKey;

    public QdrantStore(String storeName, Configuration configuration, QueryParameters queryParams, int dimension) {

        super(storeName, configuration, queryParams, dimension);

        JSONObject config = JsonUtils.readConfigFile(configuration.getConfigFilePath());
        JSONObject vectorStoreConfig = config.getJSONObject(Constants.VECTOR_STORE_QDRANT);
        String host = vectorStoreConfig.getString("QDRANT_HOST");
        String apiKey = vectorStoreConfig.getString("QDRANT_API_KEY");
        int port = vectorStoreConfig.getInt("QDRANT_GRPC_PORT");
        boolean useTls = vectorStoreConfig.getBoolean("QDRANT_USE_TLS");
        this.client = new QdrantClient(QdrantGrpcClient.newBuilder(host, port, useTls).withApiKey(apiKey).build());
        this.payloadTextKey = vectorStoreConfig.getString("QDRANT_TEXT_KEY");

        try {
            if (!this.client.collectionExistsAsync(this.storeName).get() && dimension > 0) {
                this.client.createCollectionAsync(storeName,
                        Collections.VectorParams.newBuilder().setDistance(Collections.Distance.Cosine)
                                .setSize(dimension).build())
                        .get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public EmbeddingStore<TextSegment> buildEmbeddingStore() {

        return QdrantEmbeddingStore.builder()
                .client(client)
                .payloadTextKey(payloadTextKey)
                .collectionName(storeName)
                .build();
    }

    @Override
    public JSONObject listSources() {
        try {
            // Optional max limit of 100k points.
            int MAX_POINTS = 10000;

            HashMap<String, JSONObject> sourceObjectMap = new HashMap<String, JSONObject>();
            JSONObject jsonObject = new JSONObject();

            boolean keepScrolling = true;
            Points.PointId nextOffset = null;
            List<Points.RetrievedPoint> points = new ArrayList<>(MAX_POINTS);
            while (keepScrolling && points.size() < MAX_POINTS) {
                Points.ScrollPoints.Builder request = Points.ScrollPoints.newBuilder()
                        .setCollectionName(storeName)
                        .setLimit(Math.min(queryParams.embeddingPageSize(), MAX_POINTS - points.size()));
                if (nextOffset != null) {
                    request.setOffset(nextOffset);
                }

                Points.ScrollResponse response = client.scrollAsync(request.build()).get();

                points.addAll(response.getResultList());
                nextOffset = response.getNextPageOffset();
                keepScrolling = nextOffset.hasNum() || nextOffset.hasUuid();
            }

            for (Points.RetrievedPoint point : points) {
                JSONObject metadataObject = new JSONObject(JsonFactory.toJson(point.getPayloadMap()));
                JSONObject sourceObject = getSourceObject(metadataObject);
                addOrUpdateSourceObjectIntoSourceObjectMap(sourceObjectMap, sourceObject);
            }

            jsonObject.put(Constants.JSON_KEY_SOURCES,
                    JsonUtils.jsonObjectCollectionToJsonArray(sourceObjectMap.values()));
            jsonObject.put(Constants.JSON_KEY_SOURCE_COUNT, sourceObjectMap.size());

            return jsonObject;
        } catch (ExecutionException | InterruptedException | InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}

final class JsonFactory {
    public static String toJson(Map<String, JsonWithInt.Value> map)
            throws InvalidProtocolBufferException {

        Struct.Builder structBuilder = Struct.newBuilder();
        map.forEach((key, value) -> structBuilder.putFields(key, toProtobufValue(value)));
        return JsonFormat.printer().print(structBuilder.build());
    }

    private static Value toProtobufValue(io.qdrant.client.grpc.JsonWithInt.Value value) {
        switch (value.getKindCase()) {
            case NULL_VALUE:
                return Value.newBuilder().setNullValueValue(0).build();

            case BOOL_VALUE:
                return Value.newBuilder().setBoolValue(value.getBoolValue()).build();

            case STRING_VALUE:
                return Value.newBuilder().setStringValue(value.getStringValue()).build();

            case INTEGER_VALUE:
                return Value.newBuilder().setNumberValue(value.getIntegerValue()).build();

            case DOUBLE_VALUE:
                return Value.newBuilder().setNumberValue(value.getDoubleValue()).build();

            case STRUCT_VALUE:
                Struct.Builder structBuilder = Struct.newBuilder();
                value.getStructValue()
                        .getFieldsMap()
                        .forEach(
                                (key, val) -> {
                                    structBuilder.putFields(key, toProtobufValue(val));
                                });
                return Value.newBuilder().setStructValue(structBuilder).build();

            case LIST_VALUE:
                Value.Builder listBuilder = Value.newBuilder();
                value.getListValue().getValuesList().stream()
                        .map(JsonFactory::toProtobufValue)
                        .forEach(listBuilder.getListValueBuilder()::addValues);
                return listBuilder.build();

            default:
                throw new IllegalArgumentException("Unsupported payload value type: " + value.getKindCase());
        }
    }
}
