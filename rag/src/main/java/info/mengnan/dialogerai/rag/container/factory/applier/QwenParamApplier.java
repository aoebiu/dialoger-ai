package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.community.model.dashscope.*;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import java.util.Locale;
import java.util.Map;

/**
 * 通义千问（DashScope）模型参数注入器
 */
public class QwenParamApplier extends ParamApplier {

    @Override
    protected QwenChatModel.QwenChatModelBuilder buildChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.topP(p.getDouble("topP"))
                .topK(p.getInt("topK"))
                .enableSearch(p.getBool("enableSearch"))
                .seed(p.getInt("seed"))
                .repetitionPenalty(p.getFloat("repetitionPenalty"))
                .temperature(p.getFloat("temperature"))
                .stops(p.getJSONArray("stops").toList(String.class))
                .maxTokens(p.getInt("maxTokens"))
                .isMultimodalModel(false);
        return builder;
    }

    @Override
    protected QwenStreamingChatModel.QwenStreamingChatModelBuilder buildStreamingChatModel(ModelConfig config) {
        JSONObject p = config.getParams();
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());
        builder.topP(p.getDouble("topP"))
                .topK(p.getInt("topK"))
                .enableSearch(p.getBool("enableSearch"))
                .seed(p.getInt("seed"))
                .repetitionPenalty(p.getFloat("repetitionPenalty"))
                .temperature(p.getFloat("temperature"))
                .stops(p.getJSONArray("stops").toList(String.class))
                .maxTokens(p.getInt("maxTokens"))
                .isMultimodalModel(false);
        return builder;
    }

    @Override
    protected QwenEmbeddingModel.QwenEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        JSONObject params = config.getParams();
        return QwenEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .dimension(params.getInt("dimension"));
    }

    @Override
    protected WanxImageModel.WanxImageModelBuilder buildImageModel(ModelConfig config) {
        JSONObject p = config.getParams();
        WanxImageModel.WanxImageModelBuilder builder = WanxImageModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());

        builder.refStrength(p.getFloat("refStrength"))
                .seed(p.getInt("seed"))
                .negativePrompt(p.getStr("negativePrompt"))
                .promptExtend(p.getBool("promptExtend"))
                .watermark(p.getBool("watermark"))
                .style(WanxImageStyle.of(p.getStr("style")))
                .size(WanxImageSize.of(p.getStr("size")));

        String refMode = p.getStr("refMode");
        if (refMode != null) {
            builder.refMode(WanxImageRefMode.valueOf(refMode.toUpperCase()));
        }
        return builder;
    }

    @Override
    protected QwenScoringModel.QwenScoringModelBuilder buildScoringModel(ModelConfig config) {
        JSONObject p = config.getParams();
        return QwenScoringModel.builder()
                .apiKey(config.getApiKey())
                .topN(p.getInt("topN"))
                .instruct(p.getStr("instruct"))
                .returnDocuments(false)
                .modelName(config.getModelName());
    }
}
