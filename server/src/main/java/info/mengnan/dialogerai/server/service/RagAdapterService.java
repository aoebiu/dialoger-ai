package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.container.assemble.AssembledModels;
import info.mengnan.dialogerai.rag.config.ChatAgentOptionConfig;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * RAG适配服务
 * 负责从数据库查询数据并组装成 AssembledModels
 * 作为 server 项目和 rag 项目之间的适配层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAdapterService {

    private final ChatAgentOptionRepository chatAgentOptionRepository;
    private final ModelConfigService modelConfigService;

    /**
     * 根据 optionId 查询并组装 AssembledModels
     *
     * @param optionId 聊天选项ID
     * @return AssembledModels 对象
     */
    public AssembledModels assembleModels(Long optionId) {
        ChatAgentOption chatOption = chatAgentOptionRepository.findById(optionId);
        if (chatOption == null || !chatOption.getEnabled()) {
            throw new IllegalArgumentException("找不到对应的聊天配置或配置未启用,optionId: " + optionId);
        }

        Map<ModelType, ModelConfig> modelConfigMap = modelConfigService.loadModelConfigsByAgentOptionId(optionId);
        ChatAgentOptionConfig chatOptionConfig = buildChatOptionConfig(chatOption);
        return new AssembledModels(chatOptionConfig, modelConfigMap);
    }

    private ChatAgentOptionConfig buildChatOptionConfig(ChatAgentOption chatOption) {
        ChatAgentOptionConfig config = new ChatAgentOptionConfig();
        config.setName(chatOption.getName());
        config.setTools(chatOption.getTools());
        config.setRag(chatOption.getRag());
        config.setMaxMessages(chatOption.getMaxMessages());
        config.setTransform(chatOption.getTransform());
        config.setContentAggregator(chatOption.getContentAggregator());
        config.setInDB(chatOption.getInDB());
        config.setSystemPrompt(chatOption.getSystemPrompt());
        return config;
    }
}
