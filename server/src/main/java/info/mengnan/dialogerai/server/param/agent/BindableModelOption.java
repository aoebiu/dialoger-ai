package info.mengnan.dialogerai.server.param.agent;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BindableModelOption {

    private String modelName;
    private String modelProvider;
}
