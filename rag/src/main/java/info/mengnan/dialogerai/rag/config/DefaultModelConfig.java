package info.mengnan.dialogerai.rag.config;

import lombok.Data;

@Data
@Deprecated
public class DefaultModelConfig {

    @Deprecated
    private String modelName = "qwen3-vl-plus";

    @Deprecated
    private String compressModelName = "qwen-turbo";

    public static final Long DEFAULT_OPTION_ID = 1L;
    public static final String DEFAULT_SESSION = "default-session";

}
