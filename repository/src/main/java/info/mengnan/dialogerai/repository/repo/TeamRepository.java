package info.mengnan.dialogerai.repository.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public void insert(ChatTeam entity) {
        mapper.insert(entity);
    }
}
