package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.enums.KnowledgeBaseStatus;
import info.mengnan.dialogerai.repository.enums.DocumentStatus;
import info.mengnan.dialogerai.repository.repo.DocumentInfoRepository;
import info.mengnan.dialogerai.repository.repo.KnowledgeBaseRepository;
import info.mengnan.dialogerai.server.core.DocumentEmbedding;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseCreateResponse;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseRequest;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private static final int DRAFT_RETENTION_DAYS = 1;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentInfoRepository documentInfoRepository;
    private final KnowledgeBaseBuildService knowledgeBaseBuildService;
    private final DocumentEmbedding documentEmbedding;

    public KnowledgeBaseCreateResponse create(KnowledgeBaseRequest request) {
        Long memberId = request.getMemberId();
        cleanupExpiredDrafts(memberId);

        KnowledgeBase kb = new KnowledgeBase();
        kb.setMemberId(memberId);
        kb.setName(request.getName().trim());
        kb.setDescription(request.getDescription());
        kb.setVisibility(request.getVisibility());
        kb.setStatus(KnowledgeBaseStatus.DRAFT);
        kb.setIndexName("pending");
        kb.setTopK(request.getTopK());
        kb.setScore(request.getScore());
        knowledgeBaseRepository.insert(kb);

        String indexName = documentEmbedding.buildKbIndexName(memberId, kb.getId());
        KnowledgeBase updateKb = new KnowledgeBase();
        updateKb.setId(kb.getId());
        updateKb.setIndexName(indexName);
        knowledgeBaseRepository.updateById(updateKb);
        kb.setIndexName(indexName);

        log.info("draft knowledge base created: id={}, name={}, indexName={}", kb.getId(), kb.getName(), kb.getIndexName());
        return new KnowledgeBaseCreateResponse(kb.getId(), kb.getName(), kb.getIndexName(), kb.getStatus());
    }

    public KnowledgeBaseResponse update(KnowledgeBaseRequest request) {
        KnowledgeBase kb = findById(request.getKbId(), request.getMemberId());

        KnowledgeBase updateKb = new KnowledgeBase();
        updateKb.setId(kb.getId());
        if (request.getName() != null && !request.getName().isBlank()) {
            updateKb.setName(request.getName().trim());
            kb.setName(updateKb.getName());
        }
        if (request.getDescription() != null) {
            updateKb.setDescription(request.getDescription());
            kb.setDescription(updateKb.getDescription());
        }
        if (request.getVisibility() != null && !request.getVisibility().isBlank()) {
            updateKb.setVisibility(request.getVisibility());
            kb.setVisibility(updateKb.getVisibility());
        }
        if (request.getTopK() != null && request.getScore() != null) {
            kb.setTopK(request.getTopK());
            kb.setScore(request.getScore());
        }

        knowledgeBaseRepository.updateById(updateKb);
        return KnowledgeBaseResponse.from(kb, documentInfoRepository.countByKbId(request.getKbId()));
    }

    public KnowledgeBaseResponse activateDraft(Long kbId, Long memberId) {
        KnowledgeBase kb = findById(kbId, memberId);

        if (KnowledgeBaseStatus.DRAFT.equals(kb.getStatus())) {
            KnowledgeBase updateKb = new KnowledgeBase();
            updateKb.setId(kb.getId());
            updateKb.setStatus(KnowledgeBaseStatus.ACTIVE);
            knowledgeBaseRepository.updateById(updateKb);
            kb.setStatus(KnowledgeBaseStatus.ACTIVE);
            log.info("knowledge base activated: kbId={}, name={}", kb.getId(), kb.getName());
        }
        return KnowledgeBaseResponse.from(kb, documentInfoRepository.countByKbId(kbId));
    }

    public List<KnowledgeBaseResponse> list(Long memberId) {
        List<KnowledgeBase> knowledgeBaseList = knowledgeBaseRepository.findByMemberId(memberId);
        List<Long> kbIds = knowledgeBaseList.stream().map(KnowledgeBase::getId).toList();
        Map<Long, Long> kbCountMap = documentInfoRepository.countDocsGroupedByKbIds(kbIds);
        return knowledgeBaseRepository.findByMemberId(memberId).stream()
                .map(kb -> KnowledgeBaseResponse.from(kb, kbCountMap.getOrDefault(kb.getId(), 0L)))
                .toList();
    }

    public KnowledgeBaseResponse getKnowledgeBase(Long kbId, Long memberId) {
        KnowledgeBase kb = findById(kbId, memberId);

        syncBuildProgressIfNeeded(kb, memberId);
        kb = findById(kbId, memberId);
        return KnowledgeBaseResponse.from(kb, documentInfoRepository.countByKbId(kbId));
    }

    public void deleteKnowledgeBase(Long kbId, Long memberId) {
        KnowledgeBase kb = findById(kbId, memberId);

        deleteKnowledgeBase(kb);
        log.info("knowledge base deleted: kbId={}, indexName={}", kbId, kb.getIndexName());
    }

    /**
     * 删除超过保留期的草稿知识库
     */
    public void cleanupExpiredDrafts(Long memberId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DRAFT_RETENTION_DAYS);
        List<KnowledgeBase> expired = knowledgeBaseRepository.findDraftsOlderThan(memberId, cutoff);
        for (KnowledgeBase draft : expired) {
            try {
                deleteKnowledgeBase(draft);
                log.info("expired draft knowledge base removed: kbId={}, memberId={}", draft.getId(), memberId);
            } catch (Exception e) {
                log.warn("failed to remove expired draft kb: kbId={}", draft.getId(), e);
            }
        }
    }

    public KnowledgeBase findById(Long kbId, Long memberId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        if (kb == null || !memberId.equals(kb.getMemberId()))
            throw new BusinessException(ErrorCode.KB_NOT_FOUND);
        return kb;
    }

    private void deleteKnowledgeBase(KnowledgeBase kb) {
        if (kb.getIndexName() != null && !"pending".equals(kb.getIndexName())) {
            try {
                documentEmbedding.deleteIndex(kb.getIndexName());
            } catch (Exception e) {
                log.warn("ES index deletion failed for kb: indexName={}", kb.getIndexName(), e);
            }
        }
        documentInfoRepository.findByKbId(kb.getId())
                .forEach(doc -> documentInfoRepository.deleteById(doc.getId()));
        knowledgeBaseRepository.deleteById(kb.getId());
    }

    private void syncBuildProgressIfNeeded(KnowledgeBase kb, Long memberId) {
        boolean hasActiveDocs = documentInfoRepository.findByKbId(kb.getId()).stream()
                .filter(d -> d.getDeleted() == null || d.getDeleted() == 0)
                .anyMatch(d -> {
                    String status = d.getStatus();
                    return status != null
                            && !DocumentStatus.DONE.name().equals(status)
                            && !DocumentStatus.FAILED.name().equals(status);
                });
        if (!hasActiveDocs)
            return;

        if (kb.getBuildTaskId() == null)
            knowledgeBaseBuildService.ensureBuildTask(kb.getId(), memberId);
        knowledgeBaseBuildService.refreshBuildProgress(kb.getId());
    }
}
