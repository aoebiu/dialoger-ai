package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.BizConfig;
import info.mengnan.dialogerai.repository.mapper.BizConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BizConfigRepository {

    private final BizConfigMapper mapper;

    public List<BizConfig> findByMemberId(Long memberId) {
        return mapper.findByMemberId(memberId);
    }

    public List<BizConfig> findByMemberIds(List<Long> memberIds) {
        return mapper.findByMemberIds(memberIds);
    }

    public BizConfig findByMemberAndKey(Long memberId, String configKey) {
        return mapper.findByMemberAndKey(memberId, configKey);
    }

    public void insert(BizConfig entity) {
        mapper.insert(entity);
    }

    public void updateById(BizConfig entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
