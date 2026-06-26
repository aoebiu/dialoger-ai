package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.repository.entity.BizConfig;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.repo.BizConfigRepository;
import info.mengnan.dialogerai.repository.repo.MemberRepository;
import info.mengnan.dialogerai.server.param.config.AppConfigItemResponse;
import info.mengnan.dialogerai.server.param.config.ConfigSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BizConfigService {

    private final BizConfigRepository bizConfigRepository;
    private final MemberRepository memberRepository;

    public List<AppConfigItemResponse> list(Long memberId, boolean isOwner, List<Long> teamMemberIds) {
        if (teamMemberIds == null || teamMemberIds.isEmpty())
            return List.of();

        List<BizConfig> configs = bizConfigRepository.findByMemberIds(teamMemberIds);
        List<Long> memberIds = configs.stream().map(BizConfig::getMemberId).distinct().toList();
        Map<Long, String> memberNameMap = memberRepository.findByIds(memberIds).stream()
                .collect(Collectors.toMap(ChatMember::getId, ChatMember::getUsername));

        return configs.stream()
                .map(row -> {
                    AppConfigItemResponse resp = isOwner || row.getMemberId().equals(memberId)
                            ? toMaskedResponse(row)
                            : toFullyMaskedResponse(row);
                    resp.setCreatorName(memberNameMap.get(row.getMemberId()));
                    return resp;
                })
                .toList();
    }

    public BizConfig findByMemberAndKey(Long memberId, String configKey) {
        return bizConfigRepository.findByMemberAndKey(memberId, configKey);
    }

    public AppConfigItemResponse toDetailResponse(BizConfig row) {
        AppConfigItemResponse r = toMaskedResponse(row);
        r.setConfigValue(row.getConfigValue());
        return r;
    }

    public AppConfigItemResponse toKeysMaskedDetailResponse(BizConfig row) {
        AppConfigItemResponse r = toFullyMaskedResponse(row);
        try {
            JSONObject parsed = new JSONObject(row.getConfigValue());
            parsed.keySet().forEach(key -> parsed.set(key, "****"));
            r.setConfigValue(parsed.toString());
        } catch (IllegalArgumentException ignored) {
            // 无法解析时不暴露任何内容
        }
        return r;
    }

    public AppConfigItemResponse toMaskedResponse(BizConfig row) {
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

    public BizConfig save(Long memberId, ConfigSaveRequest request) {
        BizConfig existing = bizConfigRepository.findByMemberAndKey(memberId, request.getConfigKey());
        if (existing == null) {
            BizConfig entity = new BizConfig();
            entity.setMemberId(memberId);
            entity.setConfigKey(request.getConfigKey());
            entity.setConfigValue(request.getConfigValue());
            entity.setRemark(request.getRemark());
            bizConfigRepository.insert(entity);
            return entity;
        }
        existing.setConfigValue(request.getConfigValue());
        existing.setRemark(request.getRemark());
        bizConfigRepository.updateById(existing);
        return existing;
    }

    public void delete(Long memberId, String configKey) {
        BizConfig row = bizConfigRepository.findByMemberAndKey(memberId, configKey);
        if (row != null)
            bizConfigRepository.deleteById(row.getId());
    }

    /**
     * 供业务代码读取配置值
     */
    public String getPlainValue(Long memberId, String configKey) {
        BizConfig row = bizConfigRepository.findByMemberAndKey(memberId, configKey);
        if (row == null) return null;
        return row.getConfigValue();
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

    private static String maskSecret(String value) {
        if (value == null || value.isEmpty()) return "****";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
