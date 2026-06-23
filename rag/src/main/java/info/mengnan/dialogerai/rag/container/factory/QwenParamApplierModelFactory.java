package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.container.factory.applier.QwenParamApplier;

/**
 * 通义千问（DashScope）模型工厂。
 */
public class QwenParamApplierModelFactory extends QwenParamApplier
        implements ChatModelFactory, EmbeddingModelFactory, ImageModelFactory {

    @Override
    public Object createModel(ModelConfig config, ModelType modelType) {
        return switch (modelType) {
            case CHAT           -> createChatModel(config);
            case STREAMING_CHAT -> createStreamingChatModel(config);
            case EMBEDDING      -> createEmbeddingModel(config);
            case IMAGE          -> createImageModel(config);
            default -> throw new UnsupportedOperationException(notSupported(modelType));
        };
    }

    @Override
    public QwenStreamingChatModel createStreamingChatModel(ModelConfig modelConfig) {
        return buildStreamingChatModel(modelConfig).build();
    }

    @Override
    public QwenChatModel createChatModel(ModelConfig modelConfig) {
        return buildChatModel(modelConfig).build();
    }

    @Override
    public QwenEmbeddingModel createEmbeddingModel(ModelConfig modelConfig) {
        return buildEmbeddingModel(modelConfig).build();
    }

    @Override
    public WanxImageModel createImageModel(ModelConfig modelConfig) {
        return buildImageModel(modelConfig).build();
    }
}
