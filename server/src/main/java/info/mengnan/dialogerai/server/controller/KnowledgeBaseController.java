package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseRequest;
import info.mengnan.dialogerai.server.service.KnowledgeBaseService;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final MemberService memberService;

    @PostMapping
    public R create(@RequestBody KnowledgeBaseRequest request) {
        if (request.getName() == null || request.getName().isBlank())
            return R.error(KB_NAME_EMPTY);

        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        return R.ok(knowledgeBaseService.create(request));
    }

    @PutMapping("/{kbId}")
    public R update(@PathVariable("kbId") Long kbId, @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseService.findById(kbId);
        if (kb == null)
            return R.error(KB_NOT_FOUND);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasPermission(memberId, kb.getMemberId()))
            return R.error(KB_WRITE_DENIED);

        if (request.getName() == null || request.getName().isBlank())
            return R.error(KB_NAME_EMPTY);

        return R.ok(knowledgeBaseService.update(kbId, request));

    }

    @PostMapping("/{kbId}/activate")
    public R activate(@PathVariable("kbId") Long kbId) {
        KnowledgeBase kb = knowledgeBaseService.findById(kbId);
        if (kb == null)
            return R.error(KB_NOT_FOUND);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasPermission(memberId, kb.getMemberId()))
            return R.error(KB_WRITE_DENIED);

        return R.ok(knowledgeBaseService.activateDraft(kbId));
    }

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        boolean isOwner = memberService.isOwner(memberId);
        Long teamId = memberService.resolveTeamId(memberId);
        return R.ok(knowledgeBaseService.list(memberId, isOwner, teamId));
    }

    @GetMapping("/{kbId}")
    public R get(@PathVariable("kbId") Long kbId) {
        KnowledgeBase kb = knowledgeBaseService.findById(kbId);
        if (kb == null)
            return R.error(KB_NOT_FOUND);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasPermission(memberId, kb.getMemberId()))
            return R.error(KB_WRITE_DENIED);

        return R.ok(knowledgeBaseService.getKnowledgeBase(kbId, memberId));
    }

    @DeleteMapping("/{kbId}")
    public R delete(@PathVariable("kbId") Long kbId) {
        KnowledgeBase kb = knowledgeBaseService.findById(kbId);
        if (kb == null)
            return R.error(KB_NOT_FOUND);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!hasPermission(memberId, kb.getMemberId()))
            return R.error(KB_WRITE_DENIED);

        knowledgeBaseService.deleteKnowledgeBase(kbId);
        return R.ok();
    }


    private boolean hasPermission(Long memberId, Long kbOwnerMemberId) {
        if (memberService.isOwner(memberId))
            return memberService.isTeamMember(memberService.resolveTeamId(memberId), kbOwnerMemberId);
        return memberId.equals(kbOwnerMemberId);
    }
}
