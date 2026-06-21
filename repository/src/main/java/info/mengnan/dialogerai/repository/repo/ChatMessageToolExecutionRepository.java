package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.ChatMessageToolExecution;
import info.mengnan.dialogerai.repository.mapper.ChatMessageToolExecutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatMessageToolExecutionRepository {

    private final ChatMessageToolExecutionMapper mapper;

    public void insert(ChatMessageToolExecution entity) {
        mapper.insert(entity);
    }

    public void linkToMessage(String sessionId, Long messageId) {
        mapper.linkToMessage(sessionId, messageId);
    }

    public int updateResult(String sessionId, String toolCallId, String result) {
        return mapper.updateResult(sessionId, toolCallId, result);
    }

    public List<Long> findIdsForMessage(Long messageId) {
        return mapper.findForMessage(messageId).stream()
                .map(ChatMessageToolExecution::getId)
                .toList();
    }

    public List<ChatMessageToolExecution> findToolExecutions(List<Long> ids) {
        return mapper.findToolExecutions(ids);
    }

    public Map<Long, List<Long>> findToolExecutionIdMap(String sessionId) {
        return mapper.findIdAndMessageId(sessionId).stream()
                .collect(Collectors.groupingBy(
                        ChatMessageToolExecution::getMessageId,
                        Collectors.mapping(ChatMessageToolExecution::getId, Collectors.toList())
                ));
    }

    public void deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        mapper.deleteByMessageIdGreaterThanOrEqual(sessionId, messageId);
    }
}
