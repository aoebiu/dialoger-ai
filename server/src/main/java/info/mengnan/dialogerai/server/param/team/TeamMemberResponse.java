package info.mengnan.dialogerai.server.param.team;

import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamMemberResponse {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private MemberStatus status;

    private MemberRole role;

    private LocalDateTime createdAt;
}
