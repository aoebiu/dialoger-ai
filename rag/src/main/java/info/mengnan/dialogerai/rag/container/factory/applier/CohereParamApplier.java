package info.mengnan.dialogerai.rag.container.factory.applier;

import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.rag.config.ModelConfig;

public abstract class CohereParamApplier extends ParamApplier {

    @Override
    protected CohereScoringModel.CohereScoringModelBuilder buildScoringModel(ModelConfig config) {
        JSONObject p = config.getParams();
        CohereScoringModel.CohereScoringModelBuilder builder = CohereScoringModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey()).modelName(config.getModelName());
        builder.maxRetries(p.getInt("maxRetries"));
        return builder;
    }

    protected CohereEmbeddingModel.CohereEmbeddingModelBuilder buildEmbeddingModel(ModelConfig config) {
        JSONObject p = config.getParams();
        CohereEmbeddingModel.CohereEmbeddingModelBuilder builder = CohereEmbeddingModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey());
        return builder;
    }

}
