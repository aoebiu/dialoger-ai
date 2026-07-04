package info.mengnan.dialogerai.server.param.agent;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentOptionRequest {

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

    /** keyType（如 chat）→ 模型绑定（含 modelName 与可选 params） */
    private Map<String, ModelBinding> modelBindings = new LinkedHashMap<>();

    /** 绑定的知识库 ID 列表 */
    private List<Long> kbIds = new java.util.ArrayList<>();

    private Long id;
    private Long memberId;
    private Long ownerId;
}
