package info.mengnan.dialogerai.common.validation;

import info.mengnan.dialogerai.common.param.ModelType;

import java.util.Optional;

/**
 * 按 Provider + ModelType 查找参数 Schema 与参数类。
 */
public interface ModelParamSchemaLookup {

    Optional<ModelParamSchemaDefinition> findSchema(String modelProvider, ModelType modelType);

    Optional<Class<?>> findParamClass(String modelProvider, ModelType modelType);
}
