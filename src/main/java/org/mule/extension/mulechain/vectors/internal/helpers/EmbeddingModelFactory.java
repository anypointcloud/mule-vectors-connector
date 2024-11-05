package org.mule.extension.mulechain.vectors.internal.helpers;

import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import dev.langchain4j.model.nomic.NomicEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.json.JSONObject;
import org.mule.extension.mulechain.vectors.internal.config.Configuration;
import org.mule.extension.mulechain.vectors.internal.constants.Constants;
import org.mule.extension.mulechain.vectors.internal.helpers.parameters.EmbeddingModelNameParameters;

import static org.mule.extension.mulechain.vectors.internal.util.JsonUtils.readConfigFile;

public class EmbeddingModelFactory {

    public static EmbeddingModel createModel(Configuration configuration, EmbeddingModelNameParameters modelParams) {

        EmbeddingModel model = null;
        JSONObject config = readConfigFile(configuration.getConfigFilePath());
        JSONObject llmType;
        String llmTypeKey;
        String llmTypeEndpoint;

        switch (configuration.getEmbeddingModelService()) {
            case Constants.EMBEDDING_MODEL_SERVICE_AZURE_OPENAI:
                llmType = config.getJSONObject("AZURE_OPENAI");
                llmTypeKey = llmType.getString("AZURE_OPENAI_KEY");
                llmTypeEndpoint = llmType.getString("AZURE_OPENAI_ENDPOINT");
                model = createAzureOpenAiModel(llmTypeKey, llmTypeEndpoint, modelParams);
                break;

            case Constants.EMBEDDING_MODEL_SERVICE_OPENAI:
                llmType = config.getJSONObject("OPENAI");
                llmTypeKey = llmType.getString("OPENAI_API_KEY");
                model = createOpenAiModel(llmTypeKey, modelParams);
                break;

            case Constants.EMBEDDING_MODEL_SERVICE_MISTRAL_AI:
                llmType = config.getJSONObject("MISTRAL_AI");
                llmTypeKey = llmType.getString("MISTRAL_AI_API_KEY");
                model = createMistralAIModel(llmTypeKey, modelParams);
                break;

            case Constants.EMBEDDING_MODEL_SERVICE_NOMIC:
                llmType = config.getJSONObject("NOMIC");
                llmTypeKey = llmType.getString("NOMIC_API_KEY");
                model = createNomicModel(llmTypeKey, modelParams);

                break;
            case Constants.EMBEDDING_MODEL_SERVICE_HUGGING_FACE:
                llmType = config.getJSONObject("HUGGING_FACE");
                llmTypeKey = llmType.getString("HUGGING_FACE_API_KEY");
                model = createHuggingFaceModel(llmTypeKey, modelParams);

                break;
            default:
                throw new IllegalArgumentException("Unsupported Embedding Model: " + configuration.getEmbeddingModelService());
        }
        return model;
    }

    private static EmbeddingModel createAzureOpenAiModel(String llmTypeKey, String llmTypeEndpoint, EmbeddingModelNameParameters modelParams) {
        System.out.println("Inside createAzureOpenAiModel");
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(llmTypeKey)
                .endpoint(llmTypeEndpoint)
                .deploymentName(modelParams.getEmbeddingModelName())
                .build();
    }

    private static EmbeddingModel createOpenAiModel(String llmTypeKey, EmbeddingModelNameParameters modelParams) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(llmTypeKey)
                .modelName(modelParams.getEmbeddingModelName())
                .build();
    }

    private static EmbeddingModel createMistralAIModel(String llmTypeKey, EmbeddingModelNameParameters modelParams) {
        return MistralAiEmbeddingModel.builder()
                .apiKey(llmTypeKey)
                .modelName(modelParams.getEmbeddingModelName())
                .build();
    }

    private static EmbeddingModel createNomicModel(String llmTypeKey, EmbeddingModelNameParameters modelParams) {
        return NomicEmbeddingModel.builder()
                //.baseUrl("https://api-atlas.nomic.ai/v1/")
                .apiKey(llmTypeKey)
                .modelName(modelParams.getEmbeddingModelName())
                //.taskType("clustering")
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    private static EmbeddingModel createHuggingFaceModel(String llmTypeKey, EmbeddingModelNameParameters modelParams) {
        return HuggingFaceEmbeddingModel.builder()
                .accessToken(llmTypeKey)
                .modelId(modelParams.getEmbeddingModelName())
                .build();
    }

}
