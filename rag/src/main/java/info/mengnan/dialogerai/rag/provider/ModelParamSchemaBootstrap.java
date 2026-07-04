package info.mengnan.dialogerai.rag.provider;

import info.mengnan.dialogerai.common.classpath.ClasspathScanner;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParams;

/**
 * 扫描Provider 参数类并注册到 {@link ModelParamSchemaRegistry}
 */
public final class ModelParamSchemaBootstrap {

    static final String PROVIDER_PACKAGE = "info.mengnan.dialogerai.rag.provider.params";

    private ModelParamSchemaBootstrap() {
    }

    public static void bootstrap() {
        ClasspathScanner.forEachClass(PROVIDER_PACKAGE, clazz -> {
            if (clazz.isAnnotationPresent(ModelParams.class)) {
                register(clazz);
            }
        });
    }

    static void register(Class<?> paramClass) {
        ModelParams meta = paramClass.getAnnotation(ModelParams.class);
        if (meta == null) {
            throw new IllegalArgumentException("Missing @ModelParams on " + paramClass.getName());
        }
        for (ModelType modelType : meta.types()) {
            ModelParamSchemaRegistry.bind(meta.provider(), modelType, paramClass);
        }
    }
}
