package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_member")
public class ChatMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    // private String nickname;

    private String phone;

    private String avatar;

    private MemberStatus status;

    private MemberRole role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}