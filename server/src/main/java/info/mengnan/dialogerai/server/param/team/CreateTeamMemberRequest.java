package info.mengnan.dialogerai.server.param.team;

import lombok.Data;

@Data
public class CreateTeamMemberRequest {

    private String username;

    private String password;

    private String nickname;

    private String phone;

    /** Controller 写入，非客户端提交 */
    private Long ownerId;
}
