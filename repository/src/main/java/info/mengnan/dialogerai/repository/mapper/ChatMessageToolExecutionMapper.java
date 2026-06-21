package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatMessageToolExecution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageToolExecutionMapper extends BaseMapper<ChatMessageToolExecution> {

    default int updateResult(String sessionId, String toolCallId, String result) {
        return update(null, new LambdaUpdateWrapper<ChatMessageToolExecution>()
                .set(ChatMessageToolExecution::getResult, result)
                .eq(ChatMessageToolExecution::getSessionId, sessionId)
                .eq(ChatMessageToolExecution::getToolCallId, toolCallId)
                .isNull(ChatMessageToolExecution::getMessageId));
    }

    default int linkToMessage(String sessionId, Long messageId) {
        return update(null, new LambdaUpdateWrapper<ChatMessageToolExecution>()
                .set(ChatMessageToolExecution::getMessageId, messageId)
                .eq(ChatMessageToolExecution::getSessionId, sessionId)
                .isNull(ChatMessageToolExecution::getMessageId));
    }

    default List<ChatMessageToolExecution> findForMessage(Long messageId) {
        return selectList(new LambdaQueryWrapper<ChatMessageToolExecution>()
                .eq(ChatMessageToolExecution::getMessageId, messageId));
    }

    default List<ChatMessageToolExecution> findToolExecutions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<ChatMessageToolExecution>()
                .in(ChatMessageToolExecution::getId, ids));
    }

    default List<ChatMessageToolExecution> findIdAndMessageId(String sessionId) {
        return selectList(new LambdaQueryWrapper<ChatMessageToolExecution>()
                .select(ChatMessageToolExecution::getId, ChatMessageToolExecution::getMessageId)
                .eq(ChatMessageToolExecution::getSessionId, sessionId)
                .isNotNull(ChatMessageToolExecution::getMessageId));
    }

    default void deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        delete(new LambdaQueryWrapper<ChatMessageToolExecution>()
                .eq(ChatMessageToolExecution::getSessionId, sessionId)
                .ge(ChatMessageToolExecution::getMessageId, messageId));
        delete(new LambdaQueryWrapper<ChatMessageToolExecution>()
                .eq(ChatMessageToolExecution::getSessionId, sessionId)
                .isNull(ChatMessageToolExecution::getMessageId));
    }
}
