package info.mengnan.dialogerai.repository.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import info.mengnan.dialogerai.repository.mapper.MemberRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberRelationRepository {

    private final MemberRelationMapper mapper;

    public ChatMemberRelation findByMemberId(Long memberId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatMemberRelation>()
                .eq(ChatMemberRelation::getMemberId, memberId));
    }

    public List<ChatMemberRelation> findByOwnerId(Long ownerId) {
        return mapper.selectList(new LambdaQueryWrapper<ChatMemberRelation>()
                .eq(ChatMemberRelation::getOwnerId, ownerId)
                .orderByDesc(ChatMemberRelation::getId));
    }

    public List<Long> listMemberIds(Long ownerId) {
        return findByOwnerId(ownerId).stream()
                .map(ChatMemberRelation::getMemberId)
                .toList();
    }

    public void insert(ChatMemberRelation entity) {
        mapper.insert(entity);
    }

    public void updateById(ChatMemberRelation entity) {
        mapper.updateById(entity);
    }

    public void deleteByMemberId(Long memberId) {
        mapper.delete(new LambdaQueryWrapper<ChatMemberRelation>()
                .eq(ChatMemberRelation::getMemberId, memberId));
    }
}
