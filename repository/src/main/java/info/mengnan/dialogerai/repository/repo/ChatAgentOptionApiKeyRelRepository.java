package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.ChatAgentOptionApiKeyRel;
import info.mengnan.dialogerai.repository.mapper.ChatAgentOptionApiKeyRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatAgentOptionApiKeyRelRepository {

    private final ChatAgentOptionApiKeyRelMapper mapper;

    public List<ChatAgentOptionApiKeyRel> findByChatAgentOptionId(Long chatAgentOptionId) {
        return mapper.findByChatAgentOptionId(chatAgentOptionId);
    }

    public List<ChatAgentOptionApiKeyRel> findByChatApiKeyId(Long chatApiKeyId) {
        return mapper.findByChatApiKeyId(chatApiKeyId);
    }

    public ChatAgentOptionApiKeyRel findByRelation(Long chatAgentOptionId, Long chatApiKeyId) {
        return mapper.findByRelation(chatAgentOptionId, chatApiKeyId);
    }

    public void insert(ChatAgentOptionApiKeyRel entity) {
        mapper.insert(entity);
    }

    public void update(ChatAgentOptionApiKeyRel entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public void deleteByChatAgentOptionId(Long chatAgentOptionId) {
        mapper.deleteByChatAgentOptionId(chatAgentOptionId);
    }

    public void deleteByChatApiKeyId(Long chatApiKeyId) {
        mapper.deleteByChatApiKeyId(chatApiKeyId);
    }
}
