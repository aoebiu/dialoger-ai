package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import info.mengnan.dialogerai.repository.mapper.ChatAgentOptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatAgentOptionRepository {

    private final ChatAgentOptionMapper mapper;

    public ChatAgentOption findById(Long id) {
        return mapper.findById(id);
    }

    public ChatAgentOption findByNameExact(String name) {
        return mapper.findByNameExact(name);
    }

    public List<ChatAgentOption> findByName(String name) {
        return mapper.findByName(name);
    }

    public List<ChatAgentOption> findByEnabled(Boolean enabled) {
        return mapper.findByEnabled(enabled);
    }

    public List<ChatAgentOption> findAll() {
        return mapper.selectList(null);
    }

    public List<ChatAgentOption> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public void insert(ChatAgentOption entity) {
        mapper.insert(entity);
    }

    public void update(ChatAgentOption entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
