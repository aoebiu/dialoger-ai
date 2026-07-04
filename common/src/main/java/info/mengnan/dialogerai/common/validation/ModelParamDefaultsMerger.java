package info.mengnan.dialogerai.common.validation;

import info.mengnan.dialogerai.common.json.JSONObject;

/**
 * 将 Schema 声明的默认值合并到用户提交的 params JSON
 */
public final class ModelParamDefaultsMerger {

    private ModelParamDefaultsMerger() {
    }

    public static JSONObject merge(Class<?> paramClass, JSONObject userParams) {
        ModelParamSchemaDefinition schema = ModelParamSchemaResolver.resolve(null, null, paramClass);
        JSONObject merged = new JSONObject();
        for (ModelParamFieldSchema field : schema.getFields()) {
            Object value = userParams == null ? null : userParams.get(field.getName());
            if (isConfigured(value)) {
                merged.set(field.getName(), value);
            } else if (field.getDefaultValue() != null) {
                merged.set(field.getName(), field.getDefaultValue());
            }
        }
        return merged;
    }

    private static boolean isConfigured(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return !s.isBlank();
        }
        if (value instanceof java.util.Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }
}
