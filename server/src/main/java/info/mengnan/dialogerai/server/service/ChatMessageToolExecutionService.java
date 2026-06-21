package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatMessageToolExecution;
import info.mengnan.dialogerai.repository.repo.ChatMessageToolExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatMessageToolExecutionService {

    private final ChatMessageToolExecutionRepository chatMessageToolExecutionRepository;

    public List<Long> findIdsForMessage(Long messageId) {
        return chatMessageToolExecutionRepository.findIdsForMessage(messageId);
    }

    public List<ChatMessageToolExecution> findToolExecutions(List<Long> ids) {
        return chatMessageToolExecutionRepository.findToolExecutions(ids);
    }

    public Map<Long, List<Long>> findToolExecutionIdMap(String sessionId) {
        return chatMessageToolExecutionRepository.findToolExecutionIdMap(sessionId);
    }

    public void deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        chatMessageToolExecutionRepository.deleteByMessageIdGreaterThanOrEqual(sessionId, messageId);
    }
}
