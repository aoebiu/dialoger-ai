package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.auth.*;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public R register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank())
            return R.error(MEMBER_CREDENTIALS_EMPTY);

        memberService.register(request);
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank())
            return R.error(MEMBER_CREDENTIALS_EMPTY);

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

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getOldPassword() == null || request.getOldPassword().isBlank())
                return R.error(MEMBER_PASSWORD_EMPTY);
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

        R accessError = checkOwnerAccess(ownerId, id);
        if (accessError != null)
            return accessError;

        memberService.deleteMember(id);
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
