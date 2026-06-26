package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.enums.KnowledgeBaseStatus;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    default List<KnowledgeBase> findByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0)
                .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    /** OWNER：团队所有成员的全部知识库 */
    default List<KnowledgeBase> findByMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .in(KnowledgeBase::getMemberId, memberIds)
                .eq(KnowledgeBase::getDeleted, 0)
                .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    /** MEMBER：自己所有 + 团队其他成员公开的知识库 */
    default List<KnowledgeBase> findVisibleByMemberId(Long selfMemberId, List<Long> teamMemberIds) {
        if (teamMemberIds == null || teamMemberIds.isEmpty()) {
            return findByMemberId(selfMemberId);
        }
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getDeleted, 0)
                .and(w -> w
                        .eq(KnowledgeBase::getMemberId, selfMemberId)
                        .or(o -> o
                                .in(KnowledgeBase::getMemberId, teamMemberIds)
                                .eq(KnowledgeBase::getVisibility, "public")
                        )
                )
                .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    default List<KnowledgeBase> findActiveByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0)
                .eq(KnowledgeBase::getStatus, KnowledgeBaseStatus.ACTIVE)
                .orderByDesc(KnowledgeBase::getCreatedAt));
    }

    default List<KnowledgeBase> findDraftsOlderThan(Long memberId, LocalDateTime createdBefore) {
        return selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0)
                .eq(KnowledgeBase::getStatus, KnowledgeBaseStatus.DRAFT)
                .lt(KnowledgeBase::getCreatedAt, createdBefore));
    }

    default KnowledgeBase findByIdAndMemberId(Long id, Long memberId) {
        return selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, id)
                .eq(KnowledgeBase::getMemberId, memberId)
                .eq(KnowledgeBase::getDeleted, 0));
    }

    default KnowledgeBase findByIndexName(String indexName) {
        return selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getIndexName, indexName)
                .eq(KnowledgeBase::getDeleted, 0));
    }
}
