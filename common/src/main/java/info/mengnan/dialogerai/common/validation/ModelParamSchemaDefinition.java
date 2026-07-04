package info.mengnan.dialogerai.common.validation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 某 Provider + ModelType 对应的完整参数 Schema
 */
@Data
@Builder
public class ModelParamSchemaDefinition {

    private String modelProvider;
    private String keyType;
    private String paramClass;
    private List<ModelParamFieldSchema> fields;
}
