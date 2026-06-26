package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.team.CreateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.UpdateTeamMemberRequest;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@RestController
@RequestMapping("/api/member/team")
@RequiredArgsConstructor
public class MemberRelationController {

    private final MemberService memberService;

    @PostMapping("/member")
    public R createMember(@RequestBody CreateTeamMemberRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank())
            return R.error(MEMBER_CREDENTIALS_EMPTY);

        Long ownerId = StpUtil.getLoginIdAsLong();
        R accessError = checkOwnerAccess(ownerId, null);
        if (accessError != null)
            return accessError;

        return R.ok(memberService.createTeamMember(ownerId, request));
    }

    @GetMapping("/overview")
    public R overview() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember currentUser = memberService.findById(memberId);
        if (currentUser == null)
            return R.error(MEMBER_NOT_FOUND);

        Long ownerId = memberService.resolveResourceOwnerId(memberId);
        ChatMember owner = memberService.findById(ownerId);
        if (owner == null)
            return R.error(MEMBER_NOT_FOUND);

        return R.ok(memberService.getOverview(memberId, owner));
    }

    @GetMapping("/members")
    public R listMembers() {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R accessError = checkOwnerAccess(ownerId, null);
        if (accessError != null)
            return accessError;

        return R.ok(memberService.listTeamMembers(ownerId));
    }

    @PutMapping("/member/{id}")
    public R updateMember(@PathVariable("id") Long id, @RequestBody UpdateTeamMemberRequest request) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R accessError = checkOwnerAccess(ownerId, id);
        if (accessError != null)
            return accessError;

        memberService.updateTeamMember(id, request);
        return R.ok();
    }

    @PostMapping("/member/{id}/disable")
    public R disableMember(@PathVariable("id") Long id) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        R accessError = checkOwnerAccess(ownerId, id);
        if (accessError != null)
            return accessError;

        memberService.disableMember(id);
        return R.ok();
    }

    private R checkOwnerAccess(Long ownerId, Long memberId) {
        ChatMember owner = memberService.findById(ownerId);
        if (owner == null || owner.getRole() != MemberRole.OWNER)
            return R.error(MEMBER_OWNER_REQUIRED);
        if (memberId == null)
            return null;

        ChatMemberRelation relation = memberService.findRelationByMemberId(memberId);
        if (relation == null || !relation.getOwnerId().equals(ownerId))
            return R.error(MEMBER_MANAGE_DENIED);

        ChatMember member = memberService.findById(memberId);
        if (member == null)
            return R.error(MEMBER_NOT_FOUND);
        return null;
    }
}
