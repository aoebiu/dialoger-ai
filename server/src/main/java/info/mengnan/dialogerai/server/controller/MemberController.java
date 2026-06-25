package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.auth.*;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public R register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank())
            return R.error(ErrorCode.MEMBER_CREDENTIALS_EMPTY);

        memberService.register(request);
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank())
            return R.error(ErrorCode.MEMBER_CREDENTIALS_EMPTY);

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
        return R.ok(memberService.getMemberInfo(memberId));
    }

    @PutMapping("/update")
    public R update(@RequestBody MemberUpdateRequest request) {
        if (StringUtils.hasText(request.getPassword())
                && !StringUtils.hasText(request.getOldPassword()))
            return R.error(ErrorCode.MEMBER_PASSWORD_EMPTY);

        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        memberService.updateMemberInfo(request);
        return R.ok();
    }

    @PostMapping("/delete/{id}")
    public R delete(@PathVariable("id") Long id) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        if (ownerId.equals(id))
            return R.error(ErrorCode.MEMBER_CANNOT_DELETE_SELF);

        memberService.deleteMember(ownerId, id);
        return R.ok();
    }
}
