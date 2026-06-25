package info.mengnan.dialogerai.server.param.team;

import lombok.Data;

@Data
public class CreateTeamMemberRequest {

    private String username;

    private String password;

    private String nickname;

    private String phone;
}
