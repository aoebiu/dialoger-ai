package info.mengnan.dialogerai.tool;

public interface ToolExecutionStore {

    /**
     * 工具开始执行时调用（AiServices.beforeToolExecution），message_id 尚未生成。
     */
    void savePending(String sessionId, ToolExecution execution);

    /**
     * 工具执行完成后补全 result（AiServices.afterToolExecution）。
     *
     * @return 更新的行数
     */
    boolean updateResult(String sessionId, String toolCallId, String result);

    /**
     * 将当前会话下 pending 记录绑定到 ASSISTANT 消息。
     */
    void linkToMessage(String sessionId, Long messageId);

    /**
     * 单次工具执行快照
     */
    record ToolExecution(String toolCallId, String toolName, String arguments, String result) {}
}
