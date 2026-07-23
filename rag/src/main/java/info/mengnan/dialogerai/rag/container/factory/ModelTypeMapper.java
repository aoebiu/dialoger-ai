package info.mengnan.dialogerai.rag.container.factory;

import info.mengnan.dialogerai.common.util.Cast;
import info.mengnan.dialogerai.rag.config.ModelConfig;

public enum ModelTypeMapper {
    CHAT(UniversalModelFactory::createChatModel),
    STREAMING_CHAT(UniversalModelFactory::createStreamingChatModel),
    EMBEDDING(UniversalModelFactory::createEmbeddingModel),
    MODERATION(UniversalModelFactory::createModerationModel),
    SCORING(UniversalModelFactory::createScoringModel),
    IMAGE(UniversalModelFactory::createImageModel);


    private final ModelCreator creator;

    ModelTypeMapper(ModelCreator creator) {
        this.creator = creator;
    }



    /**
     * 创建模型实例
     */
    public <T> T create(UniversalModelFactory factory, ModelConfig config) {
        return Cast.cast(creator.create(factory, config));
    }

    @FunctionalInterface
    interface ModelCreator {
        Object create(UniversalModelFactory factory, ModelConfig config);
    }
}
