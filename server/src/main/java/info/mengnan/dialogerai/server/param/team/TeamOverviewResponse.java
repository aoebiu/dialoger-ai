package info.mengnan.dialogerai.server.param.team;

import lombok.Data;

import java.util.List;

@Data
public class TeamOverviewResponse {

    private String teamName;

    private Long defaultChatModelId;

    private Long defaultImageModelId;

    /** 团队分享码（仅 Owner 可见） */
    private String shareCode;

    private TeamMemberResponse owner;

    private List<TeamMemberResponse> members;

    private Long currentUserId;
}
