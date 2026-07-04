package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * chat_agent_option 和 chat_api_key 的关联表
 * 用于表示用户的聊天模型配置启用了哪些模型（多对多关系）
 */
@Data
@TableName("chat_agent_option_api_key_rel")
public class ChatAgentOptionApiKeyRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Agent 配置 ID
     */
    private Long chatAgentOptionId;

    /**
     * API Key配置ID
     */
    private Long chatApiKeyId;

    /**
     * 该 Agent 绑定此模型时的调参 JSON，结构与 Provider 对应的 Params 类一致
     */
    private String params;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
