package info.mengnan.dialogerai.server.param.agent;

import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import lombok.Data;

@Data
public class BindableKbOption {

    private Long id;
    private String name;
    private String visibility;

    public BindableKbOption(KnowledgeBase kb) {
        this.id = kb.getId();
        this.name = kb.getName();
        this.visibility = kb.getVisibility();
    }
}
