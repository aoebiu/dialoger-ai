package info.mengnan.dialogerai.server.store;

import info.mengnan.dialogerai.repository.entity.ChatMessageToolExecution;
import info.mengnan.dialogerai.repository.repo.ChatMessageToolExecutionRepository;
import info.mengnan.dialogerai.tool.ToolExecutionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultToolExecutionStore implements ToolExecutionStore {

    private final ChatMessageToolExecutionRepository repository;

    @Override
    public void savePending(String sessionId, ToolExecution execution) {
        if (execution == null) return;
        ChatMessageToolExecution entity = new ChatMessageToolExecution();
        entity.setSessionId(sessionId);
        entity.setToolCallId(execution.toolCallId());
        entity.setToolName(execution.toolName());
        entity.setArguments(execution.arguments());
        entity.setResult(execution.result());
        repository.insert(entity);
    }

    @Override
    public boolean updateResult(String sessionId, String toolCallId, String result) {
        if (toolCallId == null || toolCallId.isBlank()) return false;
        return repository.updateResult(sessionId, toolCallId, result) > 0;
    }

    @Override
    public void linkToMessage(String sessionId, Long messageId) {
        repository.linkToMessage(sessionId, messageId);
    }
}
