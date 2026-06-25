package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.team.CreateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.UpdateTeamMemberRequest;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/team")
@RequiredArgsConstructor
public class MemberRelationController {

    private final MemberService memberService;

    @PostMapping("/member")
    public R createMember(@RequestBody CreateTeamMemberRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank())
            return R.error(ErrorCode.MEMBER_CREDENTIALS_EMPTY);

        Long ownerId = StpUtil.getLoginIdAsLong();
        return R.ok(memberService.createMember(ownerId, request));
    }

    @GetMapping("/overview")
    public R overview() {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(memberService.getOverview(memberId));
    }

    @GetMapping("/members")
    public R listMembers() {
        Long ownerId = StpUtil.getLoginIdAsLong();
        return R.ok(memberService.listMembers(ownerId));
    }

    @PutMapping("/member/{id}")
    public R updateMember(@PathVariable("id") Long id, @RequestBody UpdateTeamMemberRequest request) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        memberService.updateTeamMember(ownerId, id, request);
        return R.ok();
    }

    @PostMapping("/member/{id}/disable")
    public R disableMember(@PathVariable("id") Long id) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        memberService.disableMember(ownerId, id);
        return R.ok();
    }
}
