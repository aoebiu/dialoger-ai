package info.mengnan.dialogerai.common.validation;

import info.mengnan.dialogerai.common.util.JSONUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 从带 {@link ModelParam} 注解的参数类解析 Schema。
 */
public final class ModelParamSchemaResolver {

    private ModelParamSchemaResolver() {
    }

    public static ModelParamSchemaDefinition resolve(String modelProvider, String keyType, Class<?> paramClass) {
        List<ModelParamFieldSchema> fields = new ArrayList<>();
        for (Field field : paramClass.getDeclaredFields()) {
            ModelParam meta = field.getAnnotation(ModelParam.class);
            if (meta == null) {
                continue;
            }
            ParamValueType type = resolveType(field);
            fields.add(ModelParamFieldSchema.builder()
                    .name(fieldName(meta, field))
                    .type(type)
                    .description(meta.description())
                    .required(meta.required())
                    .defaultValue(parseDefaultValue(type, meta))
                    .min(Double.isNaN(meta.min()) ? null : meta.min())
                    .max(Double.isNaN(meta.max()) ? null : meta.max())
                    .enumValues(enumValues(field))
                    .build());
        }
        return ModelParamSchemaDefinition.builder()
                .modelProvider(modelProvider)
                .keyType(keyType)
                .paramClass(paramClass.getName())
                .fields(fields)
                .build();
    }

    static ParamValueType resolveType(Field field) {
        Class<?> type = field.getType();
        if (type.isEnum()) {
            return ParamValueType.ENUM;
        }
        if (type == String.class) {
            return ParamValueType.STRING;
        }
        if (type == Integer.class || type == int.class) {
            return ParamValueType.INTEGER;
        }
        if (type == Float.class || type == float.class) {
            return ParamValueType.FLOAT;
        }
        if (type == Double.class || type == double.class) {
            return ParamValueType.DOUBLE;
        }
        if (type == Boolean.class || type == boolean.class) {
            return ParamValueType.BOOLEAN;
        }
        if (List.class.isAssignableFrom(type) && isStringList(field)) {
            return ParamValueType.STRING_ARRAY;
        }
        throw new IllegalArgumentException("Unsupported model param field: " + field.getName());
    }

    static String fieldName(ModelParam meta, Field field) {
        return meta.name().isBlank() ? field.getName() : meta.name();
    }

    private static boolean isStringList(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return false;
        }
        Type[] args = parameterizedType.getActualTypeArguments();
        return args.length == 1 && args[0] == String.class;
    }

    private static List<String> enumValues(Field field) {
        if (!field.getType().isEnum()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (Object constant : field.getType().getEnumConstants()) {
            values.add(enumApiValue(constant));
        }
        return values;
    }

    private static String enumApiValue(Object constant) {
        try {
            Method method = constant.getClass().getMethod("getApiValue");
            return String.valueOf(method.invoke(constant));
        } catch (ReflectiveOperationException ex) {
            return ((Enum<?>) constant).name();
        }
    }

    private static Object parseDefaultValue(ParamValueType type, ModelParam meta) {
        if (meta.defaultValue().isBlank()) {
            return null;
        }
        return switch (type) {
            case STRING, ENUM -> meta.defaultValue();
            case INTEGER -> Integer.valueOf(meta.defaultValue());
            case FLOAT -> Float.valueOf(meta.defaultValue());
            case DOUBLE -> Double.valueOf(meta.defaultValue());
            case BOOLEAN -> Boolean.valueOf(meta.defaultValue());
            case STRING_ARRAY -> JSONUtil.toList(meta.defaultValue(), String.class);
        };
    }
}
