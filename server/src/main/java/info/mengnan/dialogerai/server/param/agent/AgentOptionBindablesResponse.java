package info.mengnan.dialogerai.server.param.agent;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentOptionBindablesResponse {

    private Map<String, List<BindableModelOption>> models;
    private List<BindableKbOption> kbs;

    public AgentOptionBindablesResponse(Map<String, List<BindableModelOption>> models, List<BindableKbOption> kbs) {
        this.models = models;
        this.kbs = kbs;
    }
}
