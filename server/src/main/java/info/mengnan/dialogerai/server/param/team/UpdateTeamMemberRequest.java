package info.mengnan.dialogerai.server.param.team;

import info.mengnan.dialogerai.repository.enums.MemberStatus;
import lombok.Data;

@Data
public class UpdateTeamMemberRequest {

    /** Controller 写入，非客户端提交 */
    private Long id;

    private String nickname;

    private String phone;

    private MemberStatus status;

    private String password;
}
