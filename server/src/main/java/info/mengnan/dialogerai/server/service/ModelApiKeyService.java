package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.param.ModelCapability;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.repo.ChatApiKeyRepository;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionApiKeyRelRepository;
import info.mengnan.dialogerai.server.param.apiKey.ModelApiKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelApiKeyService {

    private final ChatApiKeyRepository chatApiKeyRepository;
    private final ChatAgentOptionApiKeyRelRepository agentOptionApiKeyRelRepository;

    public ChatApiKey findById(Long id) {
        return chatApiKeyRepository.findById(id);
    }

    public List<ModelApiKeyResponse> list(Long ownerId) {
        return chatApiKeyRepository.findByMemberId(ownerId).stream()
                .map(ModelApiKeyResponse::fromMasked)
                .toList();
    }

    public ModelApiKeyResponse create(Long memberId, String modelName, String modelProvider,
                                      String keyType, String apiKey, String description,
                                      List<String> capabilities) {
        ChatApiKey entity = new ChatApiKey();
        entity.setMemberId(memberId);
        entity.setModelName(modelName.trim());
        entity.setModelProvider(modelProvider.trim());
        entity.setKeyType(keyType.trim());
        entity.setApiKey(apiKey.trim());
        if (description != null) {
            entity.setDescription(description.trim());
        }
        entity.setCapabilities(normalizeCapabilities(capabilities, keyType.trim()));
        entity.setEnabled(true);
        chatApiKeyRepository.insert(entity);
        log.info("model api key created: memberId={}, id={}, modelName={}, capabilities={}",
                memberId, entity.getId(), modelName, entity.getCapabilities());
        return ModelApiKeyResponse.from(entity);
    }

    /**
     * 仅更新指定 API Key 的能力列表。
     */
    public ModelApiKeyResponse updateCapabilities(Long id, List<String> capabilities) {
        ChatApiKey key = chatApiKeyRepository.findById(id);
        if (key == null) return null;
        key.setCapabilities(normalizeCapabilities(capabilities, key.getKeyType()));
        chatApiKeyRepository.updateById(key);
        log.info("model api key capabilities updated: id={}, capabilities={}", id, key.getCapabilities());
        return ModelApiKeyResponse.from(key);
    }

    /**
     * 更新描述与能力列表；其它字段（模型名/提供商/类型/Key）不允许修改。
     */
    public ModelApiKeyResponse update(Long id, String description, List<String> capabilities) {
        ChatApiKey key = chatApiKeyRepository.findById(id);
        if (key == null) return null;
        key.setDescription(description == null ? null : description.trim());
        key.setCapabilities(normalizeCapabilities(capabilities, key.getKeyType()));
        chatApiKeyRepository.updateById(key);
        log.info("model api key updated: id={}, capabilities={}", id, key.getCapabilities());
        return ModelApiKeyResponse.from(key);
    }

    /**
     * 校验并归一化能力列表：忽略未知项；仅 CHAT/STREAMING_CHAT 支持 vision，其他 keyType 一律清空。
     */
    private String normalizeCapabilities(List<String> capabilities, String keyType) {
        if (capabilities == null || capabilities.isEmpty()) return null;
        boolean isChat = "chat".equalsIgnoreCase(keyType) || "streaming_chat".equalsIgnoreCase(keyType);
        Set<ModelCapability> parsed = new LinkedHashSet<>();
        for (String raw : capabilities) {
            parsed.addAll(ModelCapability.parse(raw));
        }
        if (!isChat) parsed.remove(ModelCapability.VISION);
        return ModelCapability.join(parsed);
    }

    public void delete(Long ownerId, Long id) {
        chatApiKeyRepository.deleteById(id);
    }

    public boolean isBound(Long apiKeyId) {
        return !agentOptionApiKeyRelRepository.findByChatApiKeyId(apiKeyId).isEmpty();
    }

    public ModelApiKeyResponse toggleEnabled(Long id) {
        ChatApiKey key = chatApiKeyRepository.findById(id);
        if (key == null) return null;
        key.setEnabled(!Boolean.TRUE.equals(key.getEnabled()));
        chatApiKeyRepository.updateById(key);
        log.info("model api key status toggled: id={}, enabled={}", id, key.getEnabled());
        return ModelApiKeyResponse.from(key);
    }
}
