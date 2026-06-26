package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.mengnan.dialogerai.repository.entity.BizConfig;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.config.AppConfigItemResponse;
import info.mengnan.dialogerai.server.param.config.ConfigSaveRequest;
import info.mengnan.dialogerai.server.service.BizConfigService;
import info.mengnan.dialogerai.server.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 获取团队全量配置列表。
     * OWNER 看到所有成员的配置，值正常脱敏；MEMBER 看到全部配置，但其他人的值全遮掩为 ****。
     */
    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        boolean isOwner = memberService.isOwner(memberId);
        List<Long> teamIds = memberService.resolveTeamMemberIds(memberId);
        List<AppConfigItemResponse> list = bizConfigService.listByMembers(teamIds).stream()
                .map(row -> isOwner || row.getMemberId().equals(memberId)
                        ? toMaskedResponse(row)
                        : toFullyMaskedResponse(row))
                .toList();
        return R.ok(list);
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
            Optional<BizConfig> item = bizConfigService.find(memberId, key);
            return item.map(row -> R.ok(toDetailResponse(row))).orElseGet(() -> R.error("配置不存在"));
        }

        List<Long> teamIds = memberService.resolveTeamMemberIds(memberId);
        if (!teamIds.contains(targetMemberId)) {
            return R.error("配置不存在");
        }

        Optional<BizConfig> item = bizConfigService.find(targetMemberId, key);
        if (item.isEmpty()) return R.error("配置不存在");

        if (memberService.isOwner(memberId)) {
            return R.ok(toDetailResponse(item.get()));
        }
        return R.ok(toKeysMaskedDetailResponse(item.get()));
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
        request.setConfigKey(key);
        try {
            BizConfig saved = bizConfigService.save(effectiveMemberId, request);
            log.info("Member {} saved app config key={} for memberId={}", memberId, key, effectiveMemberId);
            return R.ok(toMaskedResponse(saved));
        } catch (IllegalStateException e) {
            log.warn("Save app config failed: {}", e.getMessage());
            return R.error(e.getMessage());
        }
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
        bizConfigService.delete(effectiveMemberId, key);
        return R.ok();
    }

    private Long resolveEffectiveMemberId(Long currentMemberId, Long targetMemberId) {
        if (targetMemberId == null || !memberService.isOwner(currentMemberId)) {
            return currentMemberId;
        }
        List<Long> teamIds = memberService.resolveTeamMemberIds(currentMemberId);
        if (!teamIds.contains(targetMemberId)) {
            throw new BusinessException(ErrorCode.MEMBER_MANAGE_DENIED);
        }
        return targetMemberId;
    }

    private static AppConfigItemResponse toMaskedResponse(BizConfig row) {
        AppConfigItemResponse r = new AppConfigItemResponse();
        r.setId(row.getId());
        r.setMemberId(row.getMemberId());
        r.setConfigKey(row.getConfigKey());
        r.setRemark(row.getRemark());
        r.setCreatedAt(row.getCreatedAt());
        r.setUpdatedAt(row.getUpdatedAt());
        r.setDisplayValue(maskSecret(row.getConfigValue()));
        return r;
    }

    private static AppConfigItemResponse toFullyMaskedResponse(BizConfig row) {
        AppConfigItemResponse r = new AppConfigItemResponse();
        r.setId(row.getId());
        r.setMemberId(row.getMemberId());
        r.setConfigKey(row.getConfigKey());
        r.setRemark(row.getRemark());
        r.setCreatedAt(row.getCreatedAt());
        r.setUpdatedAt(row.getUpdatedAt());
        r.setDisplayValue("****");
        return r;
    }

    /** MEMBER 查看他人配置详情：KEY 保留，VALUE 全部替换为 **** */
    private static AppConfigItemResponse toKeysMaskedDetailResponse(BizConfig row) {
        AppConfigItemResponse r = toFullyMaskedResponse(row);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(row.getConfigValue(), Map.class);
            parsed.replaceAll((k, v) -> "****");
            r.setConfigValue(OBJECT_MAPPER.writeValueAsString(parsed));
        } catch (Exception ignored) {
            // 无法解析时不暴露任何内容
        }
        return r;
    }

    private static AppConfigItemResponse toDetailResponse(BizConfig row) {
        AppConfigItemResponse r = toMaskedResponse(row);
        r.setConfigValue(row.getConfigValue());
        return r;
    }

    private static String maskSecret(String value) {
        if (value == null || value.isEmpty()) return "****";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
