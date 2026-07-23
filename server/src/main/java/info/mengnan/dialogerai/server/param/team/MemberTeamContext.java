package info.mengnan.dialogerai.server.param.team;

import info.mengnan.dialogerai.repository.enums.MemberRole;

/**
 * 当前用户所属团队的一次解析结果。
 * teamId / ownerId / role / defaultChatModelId 同源，避免调用方重复查库。
 */
public record MemberTeamContext(
        Long memberId,
        Long teamId,
        Long ownerId,
        MemberRole role,
        Long defaultChatModelId
) {
    public boolean isOwner() {
        return role == MemberRole.OWNER;
    }
}
