package info.mengnan.dialogerai.common.validation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 单个模型参数字段的 Schema 描述，返回给前端用于动态表单
 */
@Data
@Builder
public class ModelParamFieldSchema {

    private String name;
    private ParamValueType type;
    private String description;
    private boolean required;
    private Object defaultValue;
    private Double min;
    private Double max;
    private List<String> enumValues;
}
