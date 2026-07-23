package info.mengnan.dialogerai.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_team_member")
public class ChatTeamMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long memberId;

    private MemberRole role;

    private MemberStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
