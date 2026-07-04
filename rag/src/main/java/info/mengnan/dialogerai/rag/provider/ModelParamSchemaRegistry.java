package info.mengnan.dialogerai.rag.provider;

import info.mengnan.dialogerai.common.validation.ModelParamSchemaDefinition;
import info.mengnan.dialogerai.common.validation.ModelParamSchemaLookup;
import info.mengnan.dialogerai.common.validation.ModelParamSchemaResolver;
import info.mengnan.dialogerai.common.param.ModelType;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RAG 模块 Provider 参数绑定注册表。
 */
public final class ModelParamSchemaRegistry {

    public static final ModelParamSchemaLookup LOOKUP = new ModelParamSchemaLookup() {
        @Override
        public Optional<ModelParamSchemaDefinition> findSchema(String modelProvider, ModelType modelType) {
            return ModelParamSchemaRegistry.findSchema(modelProvider, modelType);
        }

        @Override
        public Optional<Class<?>> findParamClass(String modelProvider, ModelType modelType) {
            return ModelParamSchemaRegistry.findParamClass(modelProvider, modelType);
        }
    };

    private static final Map<String, Map<ModelType, Class<?>>> BINDINGS = new HashMap<>();
    private static final Map<String, ModelParamSchemaDefinition> SCHEMA_CACHE = new HashMap<>();

    static {
        ModelParamSchemaBootstrap.bootstrap();
    }

    private ModelParamSchemaRegistry() {
    }

    public static void bind(String modelProvider, ModelType modelType, Class<?> paramClass) {
        BINDINGS.computeIfAbsent(modelProvider, k -> new HashMap<>()).put(modelType, paramClass);
        SCHEMA_CACHE.put(cacheKey(modelProvider, modelType),
                ModelParamSchemaResolver.resolve(modelProvider, modelType.n(), paramClass));
    }

    public static Optional<Class<?>> findParamClass(String modelProvider, ModelType modelType) {
        Map<ModelType, Class<?>> providerBindings = BINDINGS.get(modelProvider);
        if (providerBindings == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providerBindings.get(modelType));
    }

    public static Optional<ModelParamSchemaDefinition> findSchema(String modelProvider, ModelType modelType) {
        ModelParamSchemaDefinition cached = SCHEMA_CACHE.get(cacheKey(modelProvider, modelType));
        if (cached != null) {
            return Optional.of(cached);
        }
        return findParamClass(modelProvider, modelType)
                .map(clazz -> ModelParamSchemaResolver.resolve(modelProvider, modelType.n(), clazz));
    }

    public static List<ModelParamSchemaDefinition> listSchemas(String modelProvider) {
        Map<ModelType, Class<?>> providerBindings = BINDINGS.get(modelProvider);
        if (providerBindings == null) {
            return List.of();
        }
        return providerBindings.keySet().stream()
                .map(modelType -> SCHEMA_CACHE.get(cacheKey(modelProvider, modelType)))
                .toList();
    }

    public static Map<String, Map<String, ModelParamSchemaDefinition>> listAllSchemas() {
        Map<String, Map<String, ModelParamSchemaDefinition>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<ModelType, Class<?>>> providerEntry : BINDINGS.entrySet()) {
            Map<String, ModelParamSchemaDefinition> schemasByType = new LinkedHashMap<>();
            for (ModelType modelType : providerEntry.getValue().keySet()) {
                findSchema(providerEntry.getKey(), modelType)
                        .ifPresent(schema -> schemasByType.put(modelType.n(), schema));
            }
            if (!schemasByType.isEmpty()) {
                result.put(providerEntry.getKey(), schemasByType);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Map<ModelType, Class<?>>> allBindings() {
        return Collections.unmodifiableMap(BINDINGS);
    }

    private static String cacheKey(String modelProvider, ModelType modelType) {
        return modelProvider + "#" + modelType.name();
    }
}
