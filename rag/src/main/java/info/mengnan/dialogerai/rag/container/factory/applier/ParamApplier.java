package info.mengnan.dialogerai.rag.container.factory.applier;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParams;
import info.mengnan.dialogerai.rag.config.ModelConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class ParamApplier {

    protected Object buildChatModel(ModelConfig config) { return null; }

    protected Object buildStreamingChatModel(ModelConfig config) { return null; }

    protected Object buildEmbeddingModel(ModelConfig config) { return null; }

    protected Object buildScoringModel(ModelConfig config) { return null; }

    protected Object buildModerationModel(ModelConfig config) { return null; }

    protected Object buildImageModel(ModelConfig config) { return null; }

    /**
     * 声明 Applier 使用的 Params 类，provider 与 types 信息直接从 {@link ModelParams} 注解读取。
     */
    protected abstract Set<Class<?>> paramClasses();

    /**
     * Provider 代码，从 {@link ModelParams#provider()} 自动推导。
     */
    public final String providerCode() {
        return paramClasses().stream()
                .map(c -> c.getAnnotation(ModelParams.class).provider())
                .distinct()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        getClass().getSimpleName() + " has no @ModelParams param classes"));
    }

    /**
     * ModelType → Param 类映射，从 {@link ModelParams#types()} 自动推导。
     */
    public final Map<ModelType, Class<?>> supportedParamClasses() {
        Map<ModelType, Class<?>> result = new HashMap<>();
        for (Class<?> c : paramClasses()) {
            for (ModelType t : c.getAnnotation(ModelParams.class).types()) {
                result.put(t, c);
            }
        }
        return result;
    }

    protected final String notSupported(ModelType type) {
        return getClass().getSimpleName() + " does not support model type: " + type.n();
    }
}
