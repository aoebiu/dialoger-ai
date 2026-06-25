package info.mengnan.dialogerai.server.param.team;

import lombok.Data;

import java.util.List;

@Data
public class TeamOverviewResponse {

    private TeamMemberResponse owner;

    private List<TeamMemberResponse> members;

    private Long currentUserId;
}
