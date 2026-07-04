package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.BizConfig;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.config.ConfigSaveRequest;
import info.mengnan.dialogerai.server.service.BizConfigService;
import info.mengnan.dialogerai.server.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

/**
 * 应用配置
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final BizConfigService bizConfigService;
    private final MemberService memberService;

    /**
     * 获取团队全量配置列表。
     * OWNER 看到所有成员的配置，值正常脱敏；MEMBER 看到全部配置，但其他人的值全遮掩为 ****。
     */
    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        boolean isOwner = memberService.isOwner(memberId);
        Long teamId = memberService.resolveTeamId(memberId);
        return R.ok(bizConfigService.list(memberId, isOwner, teamId));
    }

    /**
     * 获取配置详情。
     * OWNER 可通过 memberId 参数查看任意团队成员的配置（完整明文）；
     * MEMBER 可查看他人配置，但 VALUE 全部替换为 ****，只保留 KEY 名称。
     */
    @GetMapping("/{key}")
    public R getByKey(@PathVariable("key") String key,
                      @RequestParam(name = "memberId", required = false) Long targetMemberId) {
        Long memberId = StpUtil.getLoginIdAsLong();

        if (targetMemberId == null || targetMemberId.equals(memberId)) {
            BizConfig row = bizConfigService.findByMemberAndKey(memberId, key);
            if (row == null)
                return R.error(CONFIG_NOT_FOUND);
            return R.ok(bizConfigService.toDetailResponse(row));
        }

        Long teamId = memberService.resolveTeamId(memberId);
        if (!memberService.isTeamMember(teamId, targetMemberId))
            return R.error(CONFIG_NOT_FOUND);

        BizConfig row = bizConfigService.findByMemberAndKey(targetMemberId, key);
        if (row == null)
            return R.error(CONFIG_NOT_FOUND);

        if (memberService.isOwner(memberId))
            return R.ok(bizConfigService.toDetailResponse(row));
        return R.ok(bizConfigService.toKeysMaskedDetailResponse(row));
    }

    /**
     * 保存配置（新增或更新）。
     * OWNER 可通过 memberId 参数操作任意团队成员的配置；MEMBER 只能操作自己的。
     */
    @PutMapping("/{key}")
    public R save(@PathVariable("key") String key,
                  @RequestParam(name = "memberId", required = false) Long targetMemberId,
                  @Valid @RequestBody ConfigSaveRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        Long effectiveMemberId = resolveEffectiveMemberId(memberId, targetMemberId);
        if (effectiveMemberId == null)
            return R.error(MEMBER_MANAGE_DENIED);

        request.setConfigKey(key);
        BizConfig saved = bizConfigService.save(effectiveMemberId, request);
        log.info("Member {} saved app config key={} for memberId={}", memberId, key, effectiveMemberId);
        return R.ok(bizConfigService.toMaskedResponse(saved));
    }

    /**
     * 删除配置。
     * OWNER 可通过 memberId 参数删除任意团队成员的配置；MEMBER 只能删除自己的。
     */
    @DeleteMapping("/{key}")
    public R delete(@PathVariable("key") String key,
                    @RequestParam(name = "memberId", required = false) Long targetMemberId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        Long effectiveMemberId = resolveEffectiveMemberId(memberId, targetMemberId);
        if (effectiveMemberId == null)
            return R.error(MEMBER_MANAGE_DENIED);

        bizConfigService.delete(effectiveMemberId, key);
        return R.ok();
    }

    private Long resolveEffectiveMemberId(Long currentMemberId, Long targetMemberId) {
        if (targetMemberId == null || !memberService.isOwner(currentMemberId))
            return currentMemberId;
        if (!memberService.isTeamMember(memberService.resolveTeamId(currentMemberId), targetMemberId))
            return null;
        return targetMemberId;
    }
}
