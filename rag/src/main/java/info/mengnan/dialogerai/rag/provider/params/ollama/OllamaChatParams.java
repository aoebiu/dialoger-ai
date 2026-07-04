package info.mengnan.dialogerai.rag.provider.params.ollama;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParam;
import info.mengnan.dialogerai.common.validation.ModelParams;
import lombok.Data;

@Data
@ModelParams(provider = "Ollama", types = {ModelType.CHAT, ModelType.STREAMING_CHAT})
public class OllamaChatParams {

    @ModelParam(description = "采样温度", min = 0, max = 2, defaultValue = "0.7")
    private Double temperature;

    @ModelParam(name = "tpP", description = "核采样概率阈值", min = 0, max = 1, defaultValue = "0.9")
    private Double tpP;

    @ModelParam(description = "Top-K 采样", min = 0, max = 100, defaultValue = "40")
    private Integer topK;

    @ModelParam(description = "最大预测 token 数", min = 1, max = 131072, defaultValue = "2048")
    private Integer numPredict;

    @ModelParam(description = "重复惩罚", min = 0, max = 2, defaultValue = "1.1")
    private Double repeatPenalty;
}
