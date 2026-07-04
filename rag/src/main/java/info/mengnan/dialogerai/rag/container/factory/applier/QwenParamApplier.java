package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.community.model.dashscope.*;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.provider.params.qwen.QwenChatParams;
import info.mengnan.dialogerai.rag.provider.params.qwen.QwenEmbeddingParams;
import info.mengnan.dialogerai.rag.provider.params.qwen.QwenImageParams;
import info.mengnan.dialogerai.rag.provider.params.qwen.QwenScoringParams;

import java.util.Set;

/**
 * 通义千问（DashScope）模型参数注入器
 */
public class QwenParamApplier extends ParamApplier {

    @Override
    protected Set<Class<?>> paramClasses() {
        return Set.of(QwenChatParams.class, QwenEmbeddingParams.class, QwenImageParams.class, QwenScoringParams.class);
    }

    @Override
    protected QwenChatModel.QwenChatModelBuilder buildChatModel(ModelConfig config) {
        QwenChatParams params = config.getParams(QwenChatParams.class);
        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        applyQwenChatParams(builder, params);
        return builder;
    }

    @Override
    protected QwenStreamingChatModel.QwenStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        QwenChatParams params = config.getParams(QwenChatParams.class);
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        applyQwenChatParams(builder, params);
        return builder;
    }

    @Override
    protected QwenEmbeddingModel.QwenEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        QwenEmbeddingParams params = config.getParams(QwenEmbeddingParams.class);
        QwenEmbeddingModel.QwenEmbeddingModelBuilder builder = QwenEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.dimension(params.getDimension());
        return builder;
    }

    @Override
    protected WanxImageModel.WanxImageModelBuilder buildImageModel(ModelConfig config) {
        QwenImageParams params = config.getParams(QwenImageParams.class);
        WanxImageModel.WanxImageModelBuilder builder = WanxImageModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.refStrength(params.getRefStrength())
                .seed(params.getSeed())
                .negativePrompt(params.getNegativePrompt())
                .promptExtend(params.getPromptExtend())
                .watermark(params.getWatermark());
        if (params.getStyle() != null) {
            builder.style(WanxImageStyle.of(params.getStyle().getApiValue()));
        }
        if (params.getSize() != null) {
            builder.size(WanxImageSize.of(params.getSize().getApiValue()));
        }
        if (params.getRefMode() != null) {
            builder.refMode(WanxImageRefMode.valueOf(params.getRefMode().getApiValue()));
        }
        return builder;
    }

    @Override
    protected QwenScoringModel.QwenScoringModelBuilder buildScoringModel(ModelConfig config) {
        QwenScoringParams params = config.getParams(QwenScoringParams.class);
        QwenScoringModel.QwenScoringModelBuilder builder = QwenScoringModel.builder()
                .apiKey(config.getApiKey())
                .returnDocuments(false)
                .modelName(config.getModelName());
        builder.topN(params.getTopN())
                .instruct(params.getInstruct());
        return builder;
    }

    private static void applyQwenChatParams(QwenChatModel.QwenChatModelBuilder builder, QwenChatParams params) {
        builder.isMultimodalModel(false)
                .topP(params.getTopP())
                .topK(params.getTopK())
                .enableSearch(params.getEnableSearch())
                .seed(params.getSeed())
                .repetitionPenalty(params.getRepetitionPenalty())
                .temperature(params.getTemperature())
                .maxTokens(params.getMaxTokens())
                .stops(params.getStops());
    }

    private static void applyQwenChatParams(QwenStreamingChatModel.QwenStreamingChatModelBuilder builder,
                                           QwenChatParams params) {
        builder.isMultimodalModel(false)
                .topP(params.getTopP())
                .topK(params.getTopK())
                .enableSearch(params.getEnableSearch())
                .seed(params.getSeed())
                .repetitionPenalty(params.getRepetitionPenalty())
                .temperature(params.getTemperature())
                .maxTokens(params.getMaxTokens())
                .stops(params.getStops());
    }
}
