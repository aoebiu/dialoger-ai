package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatAgentOptionMapper extends BaseMapper<ChatAgentOption> {

    default ChatAgentOption findById(Long id) {
        return selectById(id);
    }

    default List<ChatAgentOption> findByEnabled(Boolean enabled) {
        LambdaQueryWrapper<ChatAgentOption> qw = new LambdaQueryWrapper<ChatAgentOption>()
                .eq(ChatAgentOption::getEnabled, enabled);
        return selectList(qw);
    }

    default List<ChatAgentOption> findByName(String name) {
        LambdaQueryWrapper<ChatAgentOption> qw = new LambdaQueryWrapper<ChatAgentOption>()
                .like(ChatAgentOption::getName, name);
        return selectList(qw);
    }

    default ChatAgentOption findByNameExact(String name) {
        LambdaQueryWrapper<ChatAgentOption> qw = new LambdaQueryWrapper<ChatAgentOption>()
                .eq(ChatAgentOption::getName, name);
        return selectOne(qw);
    }

    default List<ChatAgentOption> findByMemberId(Long memberId) {
        LambdaQueryWrapper<ChatAgentOption> qw = new LambdaQueryWrapper<ChatAgentOption>()
                .eq(ChatAgentOption::getMemberId, memberId);
        return selectList(qw);
    }
}
