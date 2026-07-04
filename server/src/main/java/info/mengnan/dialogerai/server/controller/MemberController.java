package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import info.mengnan.dialogerai.repository.entity.ChatTeam;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.auth.*;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public R register(@RequestBody RegisterRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return R.error(MEMBER_NOT_FOUND);

        memberService.register(request);
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody LoginRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return R.error(MEMBER_NOT_FOUND);

        MemberResponse memberVO = memberService.authenticate(request.getUsername(), request.getPassword());
        StpUtil.login(memberVO.getId());
        memberVO.setToken(StpUtil.getTokenValue());
        return R.ok(memberVO);
    }

    @PostMapping("/logout")
    public R logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/info")
    public R info() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember member = memberService.findById(memberId);
        if (member == null)
            return R.error(MEMBER_NOT_FOUND);

        return R.ok(memberService.toMemberResponse(member));
    }

    @PutMapping("/update")
    public R update(@RequestBody MemberUpdateRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatMember member = memberService.findById(memberId);
        if (member == null)
            return R.error(MEMBER_NOT_FOUND);

        if (StringUtils.isNotEmpty(request.getPassword())) {
            if (StringUtils.isEmpty(request.getOldPassword()))
                return R.error(MEMBER_NOT_FOUND);
            if (!memberService.matchesPassword(request.getOldPassword(), member.getPassword()))
                return R.error(MEMBER_OLD_PASSWORD_WRONG);
        }

        memberService.updateMemberInfo(memberId, request);
        return R.ok();
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        if (ownerId.equals(id))
            return R.error(MEMBER_CANNOT_DELETE_SELF);

        R ownerError = requireOwner(ownerId, id);
        if (ownerError != null)
            return ownerError;

        memberService.deleteMember(id);
        return R.ok();
    }

    private R requireOwner(Long ownerId, Long memberId) {
        ChatMember owner = memberService.findById(ownerId);
        if (owner == null || owner.getRole() != MemberRole.OWNER)
            return R.error(MEMBER_MANAGE_DENIED);

        ChatTeam team = memberService.findTeamByOwnerId(ownerId);
        if (team == null)
            return R.error(MEMBER_MANAGE_DENIED);
        if (memberId == null)
            return null;

        ChatMemberRelation relation = memberService.findRelationByMemberId(memberId);
        if (relation == null || !team.getId().equals(relation.getTeamId()))
            return R.error(MEMBER_MANAGE_DENIED);

        if (memberService.findById(memberId) == null)
            return R.error(MEMBER_NOT_FOUND);
        return null;
    }
}
