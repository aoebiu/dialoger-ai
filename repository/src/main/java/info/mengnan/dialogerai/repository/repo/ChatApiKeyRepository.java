package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.mapper.ChatApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatApiKeyRepository {

    private final ChatApiKeyMapper mapper;

    public ChatApiKey findById(Long id) {
        return mapper.selectById(id);
    }

    public List<ChatApiKey> findByIds(List<Long> ids) {
        return mapper.findByIds(ids);
    }

    public List<ChatApiKey> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public void insert(ChatApiKey entity) {
        mapper.insert(entity);
    }

    public void updateById(ChatApiKey entity) {
        mapper.updateById(entity);
    }

    public ChatApiKey findByMemberIdAndKeyTypeAndModelName(Long memberId, String keyType, String modelName) {
        return mapper.findByMemberIdAndKeyTypeAndModelName(memberId, keyType, modelName);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
