package info.mengnan.dialogerai.rag.provider.params.openai;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParam;
import info.mengnan.dialogerai.common.validation.ModelParams;
import lombok.Data;

@Data
@ModelParams(provider = "OpenAI", types = {ModelType.CHAT, ModelType.STREAMING_CHAT})
public class OpenAiChatParams {

    @ModelParam(description = "采样温度", min = 0, max = 2, defaultValue = "0.7")
    private Double temperature;

    @ModelParam(description = "核采样概率阈值", min = 0, max = 1, defaultValue = "1")
    private Double topP;

    @ModelParam(description = "最大生成 token 数", min = 1, max = 131072, defaultValue = "2048")
    private Integer maxTokens;

    @ModelParam(description = "频率惩罚", min = -2, max = 2, defaultValue = "0")
    private Double frequencyPenalty;

    @ModelParam(description = "存在惩罚", min = -2, max = 2, defaultValue = "0")
    private Double presencePenalty;

    @ModelParam(description = "随机种子")
    private Integer seed;
}
