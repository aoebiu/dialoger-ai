package info.mengnan.dialogerai.server.param.auth;

import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import lombok.Data;

@Data
public class MemberResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private MemberStatus status;
    private MemberRole role;
    private Long teamId;
    private Long ownerId;
    private String token;
}