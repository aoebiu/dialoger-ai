package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiModerationModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.provider.params.openai.OpenAiChatParams;

import java.util.Set;

/**
 * OpenAI 模型参数注入器
 * baseUrl 可通过 {@link ModelConfig#getBaseUrl()} 配置，
 * 用于对接 DeepSeek、Together AI 等 OpenAI 兼容 API
 */
public class OpenAiParamApplier extends ParamApplier {

    @Override
    protected Set<Class<?>> paramClasses() {
        return Set.of(OpenAiChatParams.class);
    }

    @Override
    protected OpenAiChatModel.OpenAiChatModelBuilder buildChatModel(ModelConfig config) {
        OpenAiChatParams params = config.getParams(OpenAiChatParams.class);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
        applyOpenAiChatParams(builder, params);
        return builder;
    }

    @Override
    protected OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        OpenAiChatParams params = config.getParams(OpenAiChatParams.class);
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
        applyOpenAiChatParams(builder, params);
        return builder;
    }

    @Override
    protected OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
    }

    @Override
    protected OpenAiModerationModel.OpenAiModerationModelBuilder buildModerationModel(ModelConfig config) {
        return OpenAiModerationModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
    }

    @Override
    protected OpenAiImageModel.OpenAiImageModelBuilder buildImageModel(ModelConfig config) {
        return OpenAiImageModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
    }

    private static void applyOpenAiChatParams(OpenAiChatModel.OpenAiChatModelBuilder builder, OpenAiChatParams params) {
        builder.temperature(params.getTemperature())
                .topP(params.getTopP())
                .maxTokens(params.getMaxTokens())
                .frequencyPenalty(params.getFrequencyPenalty())
                .presencePenalty(params.getPresencePenalty())
                .seed(params.getSeed());
    }

    private static void applyOpenAiChatParams(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
                                              OpenAiChatParams params) {
        builder.temperature(params.getTemperature())
                .topP(params.getTopP())
                .maxTokens(params.getMaxTokens())
                .frequencyPenalty(params.getFrequencyPenalty())
                .presencePenalty(params.getPresencePenalty())
                .seed(params.getSeed());
    }
}
