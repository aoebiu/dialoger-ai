package info.mengnan.dialogerai.server.param.team;

import lombok.Data;

@Data
public class UpdateTeamRequest {
    private String name;
    private Long defaultChatModelId;
    /** 团队分享码；有值时更新，空或未传则不改 */
    private String shareCode;
}
