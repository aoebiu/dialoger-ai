package info.mengnan.dialogerai.repository.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import info.mengnan.dialogerai.repository.entity.ChatTeam;
import info.mengnan.dialogerai.repository.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamRepository {

    private final TeamMapper mapper;

    public ChatTeam findById(Long id) {
        return mapper.selectById(id);
    }

    public ChatTeam findByOwnerId(Long ownerId) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatTeam>()
                .eq(ChatTeam::getOwnerId, ownerId));
    }

    public ChatTeam findByShareCode(String shareCode) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatTeam>()
                .eq(ChatTeam::getShareCode, shareCode));
    }

    public void insert(ChatTeam entity) {
        mapper.insert(entity);
    }

    public void updateById(ChatTeam entity) {
        mapper.updateById(entity);
    }

    public void update(LambdaUpdateWrapper<ChatTeam> wrapper) {
        mapper.update(null, wrapper);
    }
}
