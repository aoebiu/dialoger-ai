package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.repo.ChatApiKeyRepository;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionApiKeyRelRepository;
import info.mengnan.dialogerai.server.param.apiKey.ModelApiKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
                                      String keyType, String apiKey, String description) {
        ChatApiKey entity = new ChatApiKey();
        entity.setMemberId(memberId);
        entity.setModelName(modelName.trim());
        entity.setModelProvider(modelProvider.trim());
        entity.setKeyType(keyType.trim());
        entity.setApiKey(apiKey.trim());
        if (description != null) {
            entity.setDescription(description.trim());
        }
        entity.setEnabled(true);
        chatApiKeyRepository.insert(entity);
        log.info("model api key created: memberId={}, id={}, modelName={}", memberId, entity.getId(), modelName);
        return ModelApiKeyResponse.from(entity);
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
