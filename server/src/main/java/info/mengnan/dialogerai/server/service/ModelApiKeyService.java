package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.repo.ChatApiKeyRepository;
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

    public ChatApiKey findById(Long id) {
        return chatApiKeyRepository.findById(id);
    }

    public List<ModelApiKeyResponse> list(Long ownerId) {
        return chatApiKeyRepository.findByMemberId(ownerId).stream()
                .map(ModelApiKeyResponse::fromMasked)
                .toList();
    }

    public List<ModelApiKeyResponse> setDefaultDirectChatModel(Long ownerId, Long apiKeyId) {
        chatApiKeyRepository.clearDefaultDirectChatByMemberId(ownerId);

        ChatApiKey update = new ChatApiKey();
        update.setId(apiKeyId);
        update.setDefaultChat(true);
        chatApiKeyRepository.updateById(update);

        log.info("default direct chat model set: ownerId={}, apiKeyId={}", ownerId, apiKeyId);
        return list(ownerId);
    }

    public List<ModelApiKeyResponse> clearDefaultDirectChatModel(Long ownerId, Long apiKeyId) {
        ChatApiKey update = new ChatApiKey();
        update.setId(apiKeyId);
        update.setDefaultChat(false);
        chatApiKeyRepository.updateById(update);

        log.info("default direct chat model cleared: ownerId={}, apiKeyId={}", ownerId, apiKeyId);
        return list(ownerId);
    }

    public ModelApiKeyResponse create(Long memberId, String modelName, String modelProvider,
                                      String keyType, String apiKey) {
        ChatApiKey entity = new ChatApiKey();
        entity.setMemberId(memberId);
        entity.setModelName(modelName.trim());
        entity.setModelProvider(modelProvider.trim());
        entity.setKeyType(keyType.trim());
        entity.setApiKey(apiKey.trim());
        chatApiKeyRepository.insert(entity);
        log.info("model api key created: memberId={}, id={}, modelName={}", memberId, entity.getId(), modelName);
        return ModelApiKeyResponse.from(entity);
    }

    public void delete(Long id) {
        chatApiKeyRepository.deleteById(id);
    }
}
