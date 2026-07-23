package info.mengnan.dialogerai.server.param.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private String phone;
    /** 团队分享码，注册后自动绑定到对应团队 */
    private String shareCode;
}