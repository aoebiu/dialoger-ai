package info.mengnan.dialogerai.rag.config;

import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.common.validation.ModelParamDefaultsMerger;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型配置
 */
@Data
@NoArgsConstructor
public class ModelConfig {
    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * 模型提供商 (例如: dashscope, ollama, onnx)
     */
    private String modelProvider;

    /**
     * 模型类型 (例如: chat, streaming_chat, embedding, scoring)
     */
    private String keyType;

    /**
     * 模型调参参数
     */
    private JSONObject params = new JSONObject();


    public ModelConfig(String modelName, String apiKey, String modelProvider, String keyType) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.modelProvider = modelProvider;
        this.keyType = keyType;
    }

    /**
     * 将 params JSON 反序列化为强类型参数对象，并合并 Schema 声明的默认值。
     */
    public <T> T getParams(Class<T> paramClass) {
        JSONObject merged = ModelParamDefaultsMerger.merge(paramClass, params);
        return JSONUtil.toBean(merged.toString(), paramClass);
    }
}
