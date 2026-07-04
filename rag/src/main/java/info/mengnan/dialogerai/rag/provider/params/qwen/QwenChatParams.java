package info.mengnan.dialogerai.rag.provider.params.qwen;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParam;
import info.mengnan.dialogerai.common.validation.ModelParams;
import lombok.Data;

import java.util.List;

@Data
@ModelParams(provider = "Qwen", types = {ModelType.CHAT, ModelType.STREAMING_CHAT})
public class QwenChatParams {

    @ModelParam(description = "核采样概率阈值", min = 0, max = 1, defaultValue = "0.8")
    private Double topP;

    @ModelParam(description = "采样候选集大小", min = 0, max = 100, defaultValue = "50")
    private Integer topK;

    @ModelParam(description = "是否启用联网搜索", defaultValue = "false")
    private Boolean enableSearch;

    @ModelParam(description = "随机种子，相同 seed 可复现输出")
    private Integer seed;

    @ModelParam(description = "重复惩罚系数", min = 1, max = 2, defaultValue = "1.1")
    private Float repetitionPenalty;

    @ModelParam(description = "采样温度", min = 0, max = 2, defaultValue = "0.7")
    private Float temperature;

    @ModelParam(description = "最大生成 token 数", min = 1, max = 131072, defaultValue = "2048")
    private Integer maxTokens;

    @ModelParam(description = "停止词列表")
    private List<String> stops;
}
