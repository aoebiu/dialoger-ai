package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatApiKeyMapper extends BaseMapper<ChatApiKey> {

    default List<ChatApiKey> findByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<ChatApiKey>()
                .eq(ChatApiKey::getMemberId, memberId)
                .orderByDesc(ChatApiKey::getCreatedAt));
    }

    default List<ChatApiKey> findByIds(List<Long> ids) {
        return selectList(new LambdaQueryWrapper<ChatApiKey>()
                .in(ChatApiKey::getId, ids));
    }
}
