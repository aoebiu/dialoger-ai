package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatTeam;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.team.CreateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.UpdateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.UpdateTeamRequest;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@RestController
@RequestMapping("/api/member/team")
@RequiredArgsConstructor
public class MemberRelationController {

    private final MemberService memberService;

    @PutMapping
    public R updateTeam(@RequestBody UpdateTeamRequest request) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R ownerError = requireOwner(ownerId, null);
        if (ownerError != null) return ownerError;
        memberService.updateTeam(ownerId, request);
        return R.ok();
    }

    @PostMapping("/member")
    public R createMember(@RequestBody CreateTeamMemberRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return R.error(MEMBER_NOT_FOUND);

        Long ownerId = StpUtil.getLoginIdAsLong();
        R ownerError = requireOwner(ownerId, null);
        if (ownerError != null)
            return ownerError;

        request.setOwnerId(ownerId);
        request.setPassword(MemberController.encryptPassword(request.getPassword()));
        return R.ok(memberService.createTeamMember(request));
    }

    @GetMapping("/overview")
    public R overview() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember currentUser = memberService.findById(memberId);
        if (currentUser == null)
            return R.error(MEMBER_NOT_FOUND);

        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        ChatTeam team = ctx != null ? memberService.findTeamById(ctx.teamId()) : null;
        if (team == null)
            return R.error(MEMBER_NOT_FOUND);

        return R.ok(memberService.getOverview(memberId, team));
    }

    @GetMapping("/members")
    public R listMembers() {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R ownerError = requireOwner(ownerId, null);
        if (ownerError != null)
            return ownerError;

        MemberTeamContext ctx = memberService.resolveTeamContext(ownerId);
        return R.ok(memberService.listTeamMembers(ctx.teamId()));
    }

    @PutMapping("/member/{id}")
    public R updateMember(@PathVariable("id") Long id, @RequestBody UpdateTeamMemberRequest request) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R ownerError = requireOwner(ownerId, id);
        if (ownerError != null)
            return ownerError;

        request.setId(id);
        if (StringUtils.isNotEmpty(request.getPassword()))
            request.setPassword(MemberController.encryptPassword(request.getPassword()));
        memberService.updateTeamMember(request);
        return R.ok();
    }

    @PostMapping("/member/{id}/disable")
    public R disableMember(@PathVariable("id") Long id) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R ownerError = requireOwner(ownerId, id);
        if (ownerError != null)
            return ownerError;

        memberService.disableMember(id);
        return R.ok();
    }

    private R requireOwner(Long ownerId, Long memberId) {
        MemberTeamContext ctx = memberService.resolveTeamContext(ownerId);
        if (ctx == null || !ctx.isOwner())
            return R.error(MEMBER_MANAGE_DENIED);
        if (memberId == null)
            return null;

        if (!memberService.isTeamMember(ctx, memberId))
            return R.error(MEMBER_MANAGE_DENIED);

        if (memberService.findById(memberId) == null)
            return R.error(MEMBER_NOT_FOUND);
        return null;
    }
}
