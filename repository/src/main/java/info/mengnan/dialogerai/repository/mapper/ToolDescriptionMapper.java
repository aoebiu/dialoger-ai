package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatToolDescription;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ToolDescriptionMapper extends BaseMapper<ChatToolDescription> {

    default ChatToolDescription findByNameAndMemberId(String name, Long memberId) {
        return selectOne(new LambdaQueryWrapper<ChatToolDescription>()
                .eq(ChatToolDescription::getName, name)
                .eq(ChatToolDescription::getMemberId, memberId));
    }

    default List<ChatToolDescription> findByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapper<ChatToolDescription>()
                .eq(ChatToolDescription::getMemberId, memberId));
    }

    default List<ChatToolDescription> findByMemberIds(List<Long> memberIds) {
        return selectList(new LambdaQueryWrapper<ChatToolDescription>()
                .in(ChatToolDescription::getMemberId, memberIds)
                .orderByAsc(ChatToolDescription::getMemberId));
    }
}
