package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.knowledgebase.KnowledgeBaseRequest;
import info.mengnan.dialogerai.server.service.KnowledgeBaseService;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return R.error(ErrorCode.KB_NAME_EMPTY);

        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        return R.ok(knowledgeBaseService.create(request));
    }

    @PutMapping("/{kbId}")
    public R update(@PathVariable("kbId") Long kbId, @RequestBody KnowledgeBaseRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        request.setKbId(kbId);
        return R.ok(knowledgeBaseService.update(request));

    }

    @PostMapping("/{kbId}/activate")
    public R activate(@PathVariable("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(knowledgeBaseService.activateDraft(kbId, memberId));
    }

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        boolean isOwner = memberService.isOwner(memberId);
        List<Long> teamMemberIds = memberService.resolveTeamMemberIds(memberId);
        return R.ok(knowledgeBaseService.list(memberId, isOwner, teamMemberIds));
    }

    @GetMapping("/{kbId}")
    public R get(@PathVariable("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        boolean isOwner = memberService.isOwner(memberId);
        List<Long> teamMemberIds = memberService.resolveTeamMemberIds(memberId);
        return R.ok(knowledgeBaseService.getKnowledgeBase(kbId, memberId, isOwner, teamMemberIds));
    }

    @DeleteMapping("/{kbId}")
    public R delete(@PathVariable("kbId") Long kbId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        knowledgeBaseService.deleteKnowledgeBase(kbId, memberId);
        return R.ok();
    }
}
