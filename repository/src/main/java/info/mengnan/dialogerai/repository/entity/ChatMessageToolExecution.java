package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message_tool_execution")
public class ChatMessageToolExecution {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 chat_messages.id（ASSISTANT），由 updateMessages 阶段写入 */
    private Long messageId;

    /** 会话 ID，用于 pending → message 两阶段关联 */
    private String sessionId;

    /** LLM 工具调用 ID */
    private String toolCallId;

    private String toolName;

    private String arguments;

    private String result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
