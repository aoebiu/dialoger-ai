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
