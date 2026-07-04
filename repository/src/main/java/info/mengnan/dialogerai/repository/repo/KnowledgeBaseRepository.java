package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeBaseRepository {

    private final KnowledgeBaseMapper mapper;

    public void insert(KnowledgeBase entity) {
        mapper.insert(entity);
    }

    public void updateById(KnowledgeBase entity) {
        mapper.updateById(entity);
    }

    public KnowledgeBase findById(Long id) {
        return mapper.selectById(id);
    }

    public List<KnowledgeBase> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public List<KnowledgeBase> findByMemberIds(List<Long> memberIds) {
        return mapper.findByMemberIds(memberIds);
    }

    public List<KnowledgeBase> findPublicByMemberIds(List<Long> memberIds) {
        return mapper.findPublicByMemberIds(memberIds);
    }

    public List<KnowledgeBase> findActiveByMemberId(Long memberId) {
        return mapper.findActiveByMemberId(memberId);
    }

    public List<KnowledgeBase> findDraftsOlderThan(Long memberId, LocalDateTime createdBefore) {
        return mapper.findDraftsOlderThan(memberId, createdBefore);
    }

    public KnowledgeBase findByIndexName(String indexName) {
        return mapper.findByIndexName(indexName);
    }

    public List<KnowledgeBase> findByIds(List<Long> ids) {
        return mapper.findByIds(ids);
    }

    public void deleteById(Long id) {
        KnowledgeBase entity = new KnowledgeBase();
        entity.setId(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }
}
