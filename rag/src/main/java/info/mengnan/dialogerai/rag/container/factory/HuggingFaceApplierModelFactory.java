package info.mengnan.dialogerai.rag.container.factory;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.container.factory.applier.HuggingFaceApplier;

public class HuggingFaceApplierModelFactory extends HuggingFaceApplier
        implements ChatModelFactory, EmbeddingModelFactory {

    @Override
    public Object createModel(ModelConfig config, ModelType modelType) {
        return switch (modelType) {
            case CHAT           -> createChatModel(config);
            case STREAMING_CHAT -> createStreamingChatModel(config);
            case EMBEDDING      -> createEmbeddingModel(config);
            default -> throw new UnsupportedOperationException(notSupported(modelType));
        };
    }

    @Override
    public OpenAiStreamingChatModel createStreamingChatModel(ModelConfig modelConfig) {
        return buildStreamingChatModel(modelConfig).build();
    }

    @Override
    public OpenAiChatModel createChatModel(ModelConfig modelConfig) {
        return buildChatModel(modelConfig).build();
    }

    @Override
    public HuggingFaceEmbeddingModel createEmbeddingModel(ModelConfig modelConfig) {
        return buildEmbeddingModel(modelConfig).build();
    }
}
