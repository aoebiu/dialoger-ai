package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.rag.config.ModelConfig;

public class HuggingFaceApplier extends ParamApplier {

    private static final String BASE_URL = "https://router.huggingface.co/v1";

    @Override
    protected OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.baseUrl(config.getBaseUrl());
        if (config.getBaseUrl() == null) {
            builder.baseUrl(BASE_URL);
        }
        builder.temperature(p.getDouble("temperature"));
        builder.topP(p.getDouble("topP"));
        builder.maxTokens(p.getInt("maxTokens"));
        builder.frequencyPenalty(p.getDouble("frequencyPenalty"));
        builder.presencePenalty(p.getDouble("presencePenalty"));
        builder.seed(p.getInt("seed"));
        return builder;
    }

    @Override
    protected OpenAiChatModel.OpenAiChatModelBuilder buildChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.baseUrl(config.getBaseUrl());
        if (config.getBaseUrl() == null) {
            builder.baseUrl(BASE_URL);
        }
        builder.temperature(p.getDouble("temperature"));
        builder.topP(p.getDouble("topP"));
        builder.maxTokens(p.getInt("maxTokens"));
        builder.frequencyPenalty(p.getDouble("frequencyPenalty"));
        builder.presencePenalty(p.getDouble("presencePenalty"));
        builder.seed(p.getInt("seed"));
        return builder;

    }
    @Override
    protected HuggingFaceEmbeddingModel.HuggingFaceEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        JSONObject p = config.getParams();
        HuggingFaceEmbeddingModel.HuggingFaceEmbeddingModelBuilder builder = HuggingFaceEmbeddingModel.builder()
                .modelId(config.getModelName());

        return builder;
    }

}
