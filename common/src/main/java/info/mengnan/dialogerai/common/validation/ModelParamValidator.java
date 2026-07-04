package info.mengnan.dialogerai.common.validation;

import info.mengnan.dialogerai.common.param.ModelType;

import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.common.util.JSONUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校验前端提交的 params JSON 是否符合 Schema 定义
 */
public final class ModelParamValidator {

    private ModelParamValidator() {
    }

    public static void validate(String modelProvider, ModelType modelType, JSONObject params,
                                ModelParamSchemaLookup lookup) {
        ModelParamSchemaDefinition schema = lookup.findSchema(modelProvider, modelType).orElse(null);
        if (schema == null) {
            if (params != null && !params.isEmpty()) {
                throw new IllegalArgumentException(
                        "Provider '" + modelProvider + "' type '" + modelType.n() + "' does not accept params");
            }
            return;
        }

        JSONObject payload = params == null ? new JSONObject() : params;
        List<String> errors = new ArrayList<>(validateUnknownFields(schema, payload));

        Class<?> paramClass = lookup.findParamClass(modelProvider, modelType).orElseThrow();
        JSONObject merged = ModelParamDefaultsMerger.merge(paramClass, payload);
        Object bean;
        try {
            bean = JSONUtil.toBean(merged.toString(), paramClass);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid params: " + ex.getMessage());
        }
        errors.addAll(validateBean(bean, paramClass));

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private static List<String> validateUnknownFields(ModelParamSchemaDefinition schema, JSONObject payload) {
        Set<String> allowed = schema.getFields().stream()
                .map(ModelParamFieldSchema::getName)
                .collect(Collectors.toCollection(HashSet::new));
        List<String> errors = new ArrayList<>();
        for (String key : payload.keySet()) {
            if (!allowed.contains(key)) {
                errors.add("Unknown param: " + key);
            }
        }
        return errors;
    }

    private static List<String> validateBean(Object bean, Class<?> paramClass) {
        List<String> errors = new ArrayList<>();
        for (Field field : paramClass.getDeclaredFields()) {
            ModelParam meta = field.getAnnotation(ModelParam.class);
            if (meta == null) {
                continue;
            }
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(bean);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Cannot read field " + field.getName(), ex);
            }
            String name = ModelParamSchemaResolver.fieldName(meta, field);
            if (meta.required() && isEmpty(value)) {
                errors.add(name + " is required");
                continue;
            }
            if (isEmpty(value)) {
                continue;
            }
            validateType(name, field, value, errors);
            validateRange(name, meta, value, errors);
        }
        return errors;
    }

    private static void validateType(String name, Field field, Object value, List<String> errors) {
        ParamValueType type = ModelParamSchemaResolver.resolveType(field);
        boolean valid = switch (type) {
            case STRING -> value instanceof String;
            case ENUM -> field.getType().isInstance(value);
            case INTEGER -> value instanceof Integer;
            case FLOAT -> value instanceof Float;
            case DOUBLE -> value instanceof Double;
            case BOOLEAN -> value instanceof Boolean;
            case STRING_ARRAY -> value instanceof List<?> list && list.stream().allMatch(String.class::isInstance);
        };
        if (!valid) {
            errors.add(name + " has invalid type");
        }
    }

    private static void validateRange(String name, ModelParam meta, Object value, List<String> errors) {
        if (!(value instanceof Number number)) {
            return;
        }
        double num = number.doubleValue();
        if (!Double.isNaN(meta.min()) && num < meta.min()) {
            errors.add(name + " must be >= " + meta.min());
        }
        if (!Double.isNaN(meta.max()) && num > meta.max()) {
            errors.add(name + " must be <= " + meta.max());
        }
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }
}
