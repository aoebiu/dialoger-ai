package info.mengnan.dialogerai.server.param.auth;

import lombok.Data;

@Data
public class MemberUpdateRequest {
    private Long memberId;
    private String nickname;
    private String phone;
    private String avatar;
    private String oldPassword;
    private String password;
}