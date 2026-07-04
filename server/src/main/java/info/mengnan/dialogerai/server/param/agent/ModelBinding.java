package info.mengnan.dialogerai.server.param.agent;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 绑定的模型及其调参配置。
 */
@Data
@NoArgsConstructor
public class ModelBinding {

    /** 模型名称 */
    private String modelName;

    /** 调参 JSON，结构与 Provider 对应的 Params 类一致 */
    private String params;

    public ModelBinding(String modelName, String params) {
        this.modelName = modelName;
        this.params = params;
    }
}
