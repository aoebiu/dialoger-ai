package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver.KbIndexRef;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.DocumentInfo;
import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.enums.KnowledgeBaseStatus;
import info.mengnan.dialogerai.repository.enums.DocumentStatus;
import info.mengnan.dialogerai.repository.repo.DocumentInfoRepository;
import info.mengnan.dialogerai.repository.repo.KnowledgeBaseRepository;
import info.mengnan.dialogerai.repository.repo.MemberRepository;
import info.mengnan.dialogerai.server.core.DocumentEmbedding;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseCreateResponse;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseRequest;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentInfoRepository documentInfoRepository;
    private final KnowledgeBaseBuildService knowledgeBaseBuildService;
    private final DocumentEmbedding documentEmbedding;
    private final MemberRepository memberRepository;

    public KnowledgeBaseCreateResponse create(KnowledgeBaseRequest request) {
        Long memberId = request.getMemberId();

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

    public KnowledgeBaseResponse update(Long kbId, KnowledgeBaseRequest request) {
        KnowledgeBase updateKb = new KnowledgeBase();
        updateKb.setId(kbId);
        updateKb.setName(request.getName().trim());
        updateKb.setDescription(request.getDescription());
        updateKb.setVisibility(request.getVisibility());
        updateKb.setTopK(request.getTopK());
        updateKb.setScore(request.getScore());
        knowledgeBaseRepository.updateById(updateKb);

        KnowledgeBase kb = findById(kbId);
        return KnowledgeBaseResponse.from(kb, documentInfoRepository.countByKbId(kbId));
    }

    public KnowledgeBaseResponse activateDraft(Long kbId) {
        KnowledgeBase kb = findById(kbId);

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

    public List<KnowledgeBaseResponse> list(Long memberId, boolean isOwner, List<Long> teamMemberIds) {
        List<KnowledgeBase> knowledgeBaseList = isOwner
                ? this.listTeamKnowledgeBases(teamMemberIds)
                : this.listVisibleKnowledgeBases(memberId, teamMemberIds);


        List<Long> kbIds = knowledgeBaseList.stream().map(KnowledgeBase::getId).toList();
        Map<Long, Long> kbCountMap = documentInfoRepository.countDocsGroupedByKbIds(kbIds);

        List<Long> memberIds = knowledgeBaseList.stream().map(KnowledgeBase::getMemberId).distinct().toList();
        Map<Long, String> memberNameMap = memberRepository.findByIds(memberIds).stream()
                .collect(Collectors.toMap(ChatMember::getId, ChatMember::getUsername));

        return knowledgeBaseList.stream()
                .map(kb -> KnowledgeBaseResponse.from(kb,
                        memberNameMap.get(kb.getMemberId()),
                        kbCountMap.getOrDefault(kb.getId(), 0L)))
                .toList();
    }

    public KnowledgeBaseResponse getKnowledgeBase(Long kbId, Long memberId) {
        KnowledgeBase kb = findById(kbId);
        syncBuildProgressIfNeeded(kb, memberId);
        kb = findById(kbId);
        return KnowledgeBaseResponse.from(kb, documentInfoRepository.countByKbId(kbId));
    }

    public void deleteKnowledgeBase(Long kbId) {
        KnowledgeBase kb = findById(kbId);
        deleteKnowledgeBase(kb);
        log.info("knowledge base deleted: kbId={}, indexName={}", kbId, kb.getIndexName());
    }


    public KnowledgeBase findById(Long kbId) {
        return knowledgeBaseRepository.findById(kbId);
    }

    /** 当前用户可见且已激活的知识库，供 Agent 绑定选择 */
    public List<KnowledgeBase> listVisibleActive(Long memberId, boolean isOwner, List<Long> teamMemberIds) {
        List<KnowledgeBase> knowledgeBases = isOwner
                ? listTeamKnowledgeBases(new ArrayList<>(teamMemberIds))
                : listVisibleKnowledgeBases(memberId, new ArrayList<>(teamMemberIds));

        return knowledgeBases.stream()
                .filter(kb -> KnowledgeBaseStatus.ACTIVE.equals(kb.getStatus()))
                .toList();
    }

    /** 按绑定顺序解析已激活知识库的 RAG 索引引用 */
    public List<KbIndexRef> resolveActiveKbIndexRefs(List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty())
            return List.of();

        Map<Long, KnowledgeBase> kbMap = knowledgeBaseRepository.findByIds(kbIds).stream()
                .collect(Collectors.toMap(KnowledgeBase::getId, kb -> kb));

        List<KbIndexRef> refs = new ArrayList<>();
        for (Long kbId : kbIds) {
            KnowledgeBase kb = kbMap.get(kbId);
            if (kb == null || !KnowledgeBaseStatus.ACTIVE.equals(kb.getStatus()))
                continue;
            refs.add(new KbIndexRef(kb.getIndexName(), kb.getName(), kb.getTopK(), kb.getScore()));
        }
        return refs;
    }

    private List<KnowledgeBase> listTeamKnowledgeBases(List<Long> teamMemberIds) {
        if (teamMemberIds == null || teamMemberIds.isEmpty())
            return List.of();
        return knowledgeBaseRepository.findByMemberIds(teamMemberIds);
    }

    /** MEMBER：自己所有 + 团队其他成员公开的知识库 */
    private List<KnowledgeBase> listVisibleKnowledgeBases(Long memberId, List<Long> teamMemberIds) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseRepository.findByMemberId(memberId);
        if (teamMemberIds == null || teamMemberIds.isEmpty())
            return knowledgeBases;

        teamMemberIds.remove(memberId);
        if (!teamMemberIds.isEmpty())
            knowledgeBases.addAll(knowledgeBaseRepository.findPublicByMemberIds(teamMemberIds));

        return knowledgeBases.stream()
                .sorted(Comparator.comparing(KnowledgeBase::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void deleteKnowledgeBase(KnowledgeBase kb) {
        if (kb.getIndexName() != null && !"pending".equals(kb.getIndexName())) {
            try {
                documentEmbedding.deleteIndex(kb.getIndexName());
            } catch (Exception e) {
                log.warn("ES index deletion failed for kb: indexName={}", kb.getIndexName(), e);
            }
        }
        List<Long> ids = documentInfoRepository.findByKbId(kb.getId()).stream().map(DocumentInfo::getId).toList();
        documentInfoRepository.deleteByIds(ids);
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
