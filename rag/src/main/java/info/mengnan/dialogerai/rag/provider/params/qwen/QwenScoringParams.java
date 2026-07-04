package info.mengnan.dialogerai.rag.provider.params.qwen;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParam;
import info.mengnan.dialogerai.common.validation.ModelParams;
import lombok.Data;

@Data
@ModelParams(provider = "Qwen", types = ModelType.SCORING)
public class QwenScoringParams {

    @ModelParam(description = "返回 Top-N 结果", min = 1, max = 100, defaultValue = "3")
    private Integer topN;

    @ModelParam(description = "排序指令提示词")
    private String instruct;
}
