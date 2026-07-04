package info.mengnan.dialogerai.rag.provider.params.qwen;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParam;
import info.mengnan.dialogerai.common.validation.ModelParams;
import lombok.Data;

@Data
@ModelParams(provider = "Qwen", types = ModelType.EMBEDDING)
public class QwenEmbeddingParams {

    @ModelParam(description = "向量维度", min = 1, max = 4096, defaultValue = "1536")
    private Integer dimension;
}
