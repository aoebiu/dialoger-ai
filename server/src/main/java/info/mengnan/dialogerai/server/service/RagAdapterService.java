package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.rag.container.assemble.AssembledModels;
import info.mengnan.dialogerai.rag.config.ChatOptionConfig;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.repository.entity.ChatOption;
import info.mengnan.dialogerai.repository.repo.ChatOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

    private final ChatOptionRepository chatOptionService;
    private final ModelConfigService modelConfigService;

    /**
     * 根据 optionId 查询并组装 AssembledModels
     *
     * @param optionId 聊天选项ID
     * @return AssembledModels 对象
     */
    public AssembledModels assembleModels(Long optionId) {
        // 查询聊天选项
        ChatOption chatOption = chatOptionService.findById(optionId);
        if (chatOption == null || !chatOption.getEnabled()) {
            throw new IllegalArgumentException("找不到对应的聊天配置或配置未启用,optionId: " + optionId);
        }

        Map<ModelType, ModelConfig> modelConfigMap = modelConfigService.loadModelConfigs(chatOption.getMemberId());
        // 组装并返回
        ChatOptionConfig chatOptionConfig = buildChatOptionConfig(chatOption);
        return new AssembledModels(chatOptionConfig, modelConfigMap);
    }

    private ChatOptionConfig buildChatOptionConfig(ChatOption chatOption) {
        ChatOptionConfig config = new ChatOptionConfig();
        config.setName(chatOption.getName());
        config.setTools(chatOption.getTools());
        config.setRag(chatOption.getRag());
        config.setMaxMessages(chatOption.getMaxMessages());
        config.setTransform(chatOption.getTransform());
        config.setContentAggregator(chatOption.getContentAggregator());
        config.setContentInjectorPrompt(chatOption.getContentInjectorPrompt());
        config.setInDB(chatOption.getInDB());
        return config;
    }

    private Map<String, Object> parseParams(String paramJson) {
        if (paramJson == null || paramJson.isBlank()) {
            return new HashMap<>();
        }
        return JSONUtil.parseObj(paramJson);
    }
}