package info.mengnan.dialogerai.server.param.team;

import info.mengnan.dialogerai.repository.enums.MemberRole;

/**
 * 当前用户所属团队相关信息
 */
public record MemberTeamContext(
        Long memberId,
        Long teamId,
        Long ownerId,
        MemberRole role,
        Long defaultChatModelId,
        Long defaultImageModelId
) {
    public boolean isOwner() {
        return role == MemberRole.OWNER;
    }
}
