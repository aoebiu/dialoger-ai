package info.mengnan.dialogerai.server.param.chat;

import info.mengnan.dialogerai.repository.entity.ChatMessageToolExecution;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolExecutionDto {

    private Long id;
    private String toolCallId;
    private String toolName;
    private String arguments;
    private String result;

    public static ToolExecutionDto from(ChatMessageToolExecution execution) {
        return new ToolExecutionDto(
                execution.getId(),
                execution.getToolCallId(),
                execution.getToolName(),
                execution.getArguments(),
                execution.getResult()
        );
    }
}
