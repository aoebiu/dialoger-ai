package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_team")
public class ChatTeam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerId;

    private String name;

    /** 团队分享码，用于注册时绑定到该团队 */
    @TableField("share_code")
    private String shareCode;

    private Long defaultChatModelId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
