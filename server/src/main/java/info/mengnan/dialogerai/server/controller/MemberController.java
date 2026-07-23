package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.auth.*;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

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
        if (StringUtils.isEmpty(request.getShareCode()))
            return R.error(SHARE_CODE_REQUIRED);

        request.setPassword(encryptPassword(request.getPassword()));
        memberService.register(request);
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody LoginRequest request) {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()))
            return R.error(MEMBER_NOT_FOUND);

        MemberResponse memberVO = memberService.authenticate(
                request.getUsername(), encryptPassword(request.getPassword()));
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

        return R.ok(memberService.buildMemberResponse(member));
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
            if (!matchesPassword(request.getOldPassword(), member.getPassword()))
                return R.error(MEMBER_OLD_PASSWORD_WRONG);
            request.setPassword(encryptPassword(request.getPassword()));
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

    private boolean matchesPassword(String rawPassword, String encryptedPassword) {
        return encryptPassword(rawPassword).equals(encryptedPassword);
    }

    static String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
