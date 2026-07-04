package info.mengnan.dialogerai.server.param.agent;

import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentOptionResponse {

    private Long id;
    private Long memberId;
    private String name;
    private Integer maxMessages;
    private Boolean enabled;
    private Boolean rag;
    private String transform;
    private Boolean contentAggregator;
    private Boolean tools;
    private String contentInjectorPrompt;
    private String systemPrompt;
    private Boolean inDB;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** keyType（如 chat）→ 模型绑定（含 modelName 与可选 params） */
    private Map<String, ModelBinding> modelBindings = new LinkedHashMap<>();

    /** 绑定的知识库 ID 列表 */
    private List<Long> kbIds;

    public AgentOptionResponse(ChatAgentOption option) {
        this.id = option.getId();
        this.memberId = option.getMemberId();
        this.name = option.getName();
        this.maxMessages = option.getMaxMessages();
        this.enabled = option.getEnabled();
        this.rag = option.getRag();
        this.transform = option.getTransform();
        this.contentAggregator = option.getContentAggregator();
        this.tools = option.getTools();
        this.contentInjectorPrompt = option.getContentInjectorPrompt();
        this.systemPrompt = option.getSystemPrompt();
        this.inDB = option.getInDB();
        this.remark = option.getRemark();
        this.kbIds = option.getKbIds() != null ? option.getKbIds() : new ArrayList<>();
        this.createdAt = option.getCreatedAt();
        this.updatedAt = option.getUpdatedAt();
    }

    public AgentOptionResponse(ChatAgentOption option, Map<String, ModelBinding> modelBindings) {
        this(option);
        this.modelBindings = modelBindings != null ? modelBindings : new LinkedHashMap<>();
    }
}
