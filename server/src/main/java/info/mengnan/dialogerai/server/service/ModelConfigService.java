package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.repository.entity.ChatAgentOptionApiKeyRel;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionApiKeyRelRepository;
import info.mengnan.dialogerai.repository.repo.ChatApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型配置服务
 * 负责从数据库动态查询模型配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ChatApiKeyRepository chatApiKeyRepository;
    private final ChatAgentOptionApiKeyRelRepository chatAgentOptionApiKeyRelRepository;

    /**
     * 根据模型名称和类型从数据库查询模型配置
     */
    public ModelConfig findModel(Long memberId, String modelName, ModelType modelType) {
        if (modelName == null || modelType == null) {
            log.warn("Model name or model type is null");
            return null;
        }

        try {
            List<ChatApiKey> apiKeys = chatApiKeyRepository.findByMemberId(memberId);

            ChatApiKey matchedKey = apiKeys.stream()
                    .filter(key -> modelType.n().equals(key.getKeyType()) && modelName.equals(key.getModelName()))
                    .findFirst()
                    .orElse(null);

            if (matchedKey == null) {
                log.warn("Model not found in database: name={}, type={}", modelName, modelType);
                return null;
            }

            return buildModelConfig(matchedKey, null);
        } catch (Exception e) {
            log.error("Failed to query model config from database: name={}, type={}", modelName, modelType, e);
            return null;
        }
    }

    public String findDefaultDirectChatModelName(Long ownerId) {
        if (ownerId == null)
            return null;
        ChatApiKey key = chatApiKeyRepository.findDefaultDirectChatByMemberId(ownerId);
        return key != null ? key.getModelName() : null;
    }

    public Map<ModelType, ModelConfig> loadModelConfigs(Long memberId) {
        return chatApiKeyRepository.findByMemberId(memberId).stream()
                .collect(Collectors.toMap(
                        chatApiKey -> ModelType.valueOf(chatApiKey.getKeyType().toUpperCase()),
                        key -> buildModelConfig(key, null),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 根据 Agent 绑定的 API Key 关联加载模型配置，params 取自关联表。
     */
    public Map<ModelType, ModelConfig> loadModelConfigsByAgentOptionId(Long agentOptionId) {
        List<ChatAgentOptionApiKeyRel> rels = chatAgentOptionApiKeyRelRepository.findByChatAgentOptionId(agentOptionId);
        if (rels.isEmpty()) {
            return Map.of();
        }

        List<Long> apiKeyIds = rels.stream().map(ChatAgentOptionApiKeyRel::getChatApiKeyId).toList();
        Map<Long, ChatApiKey> apiKeyMap = chatApiKeyRepository.findByIds(apiKeyIds).stream()
                .collect(Collectors.toMap(ChatApiKey::getId, Function.identity()));

        return rels.stream()
                .map(rel -> {
                    ChatApiKey apiKey = apiKeyMap.get(rel.getChatApiKeyId());
                    if (apiKey == null) {
                        return null;
                    }
                    return buildModelConfig(apiKey, rel.getParams());
                })
                .filter(config -> config != null)
                .collect(Collectors.toMap(
                        config -> ModelType.valueOf(config.getKeyType().toUpperCase()),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    private ModelConfig buildModelConfig(ChatApiKey apiKey, String paramsJson) {
        ModelConfig config = new ModelConfig();
        config.setModelName(apiKey.getModelName());
        config.setBaseUrl(apiKey.getBaseUrl());
        config.setApiKey(apiKey.getApiKey());
        config.setModelProvider(apiKey.getModelProvider());
        config.setKeyType(apiKey.getKeyType());
        if (paramsJson != null && !paramsJson.isBlank()) {
            config.setParams(new JSONObject(paramsJson));
        }
        return config;
    }
}
