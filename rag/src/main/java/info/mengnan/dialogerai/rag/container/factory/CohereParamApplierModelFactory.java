package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.container.factory.applier.CohereParamApplier;

public class CohereParamApplierModelFactory extends CohereParamApplier
        implements ScoringModelFactory,EmbeddingModelFactory {
    @Override
    public Object createModel(ModelConfig config, ModelType modelType) {
        return switch (modelType) {
            case EMBEDDING    -> createEmbeddingModel(config);
            case SCORING      -> createScoringModel(config);
            default -> throw new UnsupportedOperationException(notSupported(modelType));
        };
    }

    @Override
    public CohereScoringModel createScoringModel(ModelConfig modelConfig) {
        return buildScoringModel(modelConfig).build();
    }

    @Override
    public CohereEmbeddingModel createEmbeddingModel(ModelConfig modelConfig) {
        return buildEmbeddingModel(modelConfig).build();
    }
}
