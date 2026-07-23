package info.mengnan.dialogerai.server.param.team;

import info.mengnan.dialogerai.repository.enums.MemberStatus;
import lombok.Data;

@Data
public class UpdateTeamMemberRequest {

    private Long id;

    private String nickname;

    private String phone;

    private MemberStatus status;

    private String password;
}
