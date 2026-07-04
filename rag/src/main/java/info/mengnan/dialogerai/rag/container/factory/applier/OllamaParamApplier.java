package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.provider.params.ollama.OllamaChatParams;

import java.util.Set;

/**
 * Ollama 模型参数注入器
 */
public class OllamaParamApplier extends ParamApplier {

    @Override
    protected Set<Class<?>> paramClasses() {
        return Set.of(OllamaChatParams.class);
    }

    @Override
    protected OllamaChatModel.OllamaChatModelBuilder buildChatModel(ModelConfig config) {
        OllamaChatParams params = config.getParams(OllamaChatParams.class);
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
        applyOllamaChatParams(builder, params);
        return builder;
    }

    @Override
    protected OllamaStreamingChatModel.OllamaStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        OllamaChatParams params = config.getParams(OllamaChatParams.class);
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
        applyOllamaChatParams(builder, params);
        return builder;
    }

    @Override
    protected OllamaEmbeddingModel.OllamaEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        return OllamaEmbeddingModel.builder()
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl());
    }

    private static void applyOllamaChatParams(OllamaChatModel.OllamaChatModelBuilder builder, OllamaChatParams params) {
        builder.temperature(params.getTemperature())
                .topP(params.getTpP())
                .topK(params.getTopK())
                .numPredict(params.getNumPredict())
                .repeatPenalty(params.getRepeatPenalty());
    }

    private static void applyOllamaChatParams(OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder,
                                              OllamaChatParams params) {
        builder.temperature(params.getTemperature())
                .topP(params.getTpP())
                .topK(params.getTopK())
                .numPredict(params.getNumPredict())
                .repeatPenalty(params.getRepeatPenalty());
    }
}
