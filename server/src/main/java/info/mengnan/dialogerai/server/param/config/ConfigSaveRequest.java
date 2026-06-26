package info.mengnan.dialogerai.server.param.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigSaveRequest {

    @Size(max = 191)
    private String configKey;

    @NotBlank
    @Size(max = 8000)
    private String configValue;

    @Size(max = 500)
    private String remark;
}
