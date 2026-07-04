package info.mengnan.dialogerai.rag.container.assemble;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ChatAgentOptionConfig;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import lombok.Data;

import java.util.Map;

@Data
public class AssembledModels {

    private String name;  // 模型名称
    private Boolean tools; // 是否开启tools
    private Boolean rag;  // 是否开启rag
    private Integer maxMessages; // 最大消息数量
    private String transform;    // transform类型
    private Boolean contentAggregator; // 是否开启聚合排序
    private Boolean inDB;
    private String systemPrompt;
    private Map<ModelType, ModelConfig> configs;

    public AssembledModels(ChatAgentOptionConfig config, Map<ModelType, ModelConfig> modelConfigMap) {
        if (config == null) {
            throw new IllegalArgumentException("Invalid chat option config");
        }
        this.setName(config.getName());
        this.setTools(config.getTools());
        this.setRag(config.getRag());
        this.setMaxMessages(config.getMaxMessages());
        this.setTransform(config.getTransform());
        this.setContentAggregator(config.getContentAggregator());
        this.setInDB(config.getInDB());
        this.setSystemPrompt(config.getSystemPrompt());
        this.setConfigs(modelConfigMap);
    }
}