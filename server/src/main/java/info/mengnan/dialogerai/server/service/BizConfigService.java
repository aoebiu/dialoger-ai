package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.BizConfig;
import info.mengnan.dialogerai.repository.repo.BizConfigRepository;
import info.mengnan.dialogerai.server.param.config.ConfigSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BizConfigService {

    private final BizConfigRepository bizConfigRepository;

    public List<BizConfig> listByMember(Long memberId) {
        return bizConfigRepository.listByMember(memberId);
    }

    public List<BizConfig> listByMembers(List<Long> memberIds) {
        return bizConfigRepository.listByMembers(memberIds);
    }

    public Optional<BizConfig> find(Long memberId, String configKey) {
        BizConfig row = bizConfigRepository.findByMemberAndKey(memberId, configKey);
        return Optional.ofNullable(row);
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
        if (request.getRemark() != null) {
            existing.setRemark(request.getRemark());
        }
        bizConfigRepository.update(existing);
        return existing;
    }

    public void delete(Long memberId, String configKey) {
        bizConfigRepository.deleteByMemberAndKey(memberId, configKey);
    }

    /**
     * 供业务代码读取配置值
     */
    public String getPlainValue(Long memberId, String configKey) {
        BizConfig row = bizConfigRepository.findByMemberAndKey(memberId, configKey);
        if (row == null) return null;
        return row.getConfigValue();
    }
}
