package info.mengnan.dialogerai.repository.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.mengnan.dialogerai.repository.entity.ChatTeamMember;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.mapper.TeamMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TeamMemberRepository {

    private final TeamMemberMapper mapper;

    public ChatTeamMember findByMemberId(Long memberId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatTeamMember>()
                .eq(ChatTeamMember::getMemberId, memberId));
    }

    public List<ChatTeamMember> findByTeamId(Long teamId) {
        return mapper.selectList(new LambdaQueryWrapper<ChatTeamMember>()
                .eq(ChatTeamMember::getTeamId, teamId)
                .orderByAsc(ChatTeamMember::getRole)
                .orderByDesc(ChatTeamMember::getId));
    }

    public List<ChatTeamMember> findMembersByTeamId(Long teamId) {
        return mapper.selectList(new LambdaQueryWrapper<ChatTeamMember>()
                .eq(ChatTeamMember::getTeamId, teamId)
                .eq(ChatTeamMember::getRole, MemberRole.MEMBER)
                .orderByDesc(ChatTeamMember::getId));
    }

    public List<Long> listMemberIds(Long teamId) {
        return findByTeamId(teamId).stream()
                .map(ChatTeamMember::getMemberId)
                .toList();
    }

    public void insert(ChatTeamMember entity) {
        mapper.insert(entity);
    }

    public void updateById(ChatTeamMember entity) {
        mapper.updateById(entity);
    }

    public void deleteByMemberId(Long memberId) {
        mapper.delete(new LambdaQueryWrapper<ChatTeamMember>()
                .eq(ChatTeamMember::getMemberId, memberId));
    }
}
