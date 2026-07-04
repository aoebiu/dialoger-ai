package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatAgentOptionApiKeyRel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatAgentOptionApiKeyRelMapper extends BaseMapper<ChatAgentOptionApiKeyRel> {

    default List<ChatAgentOptionApiKeyRel> findByChatAgentOptionId(Long chatAgentOptionId) {
        LambdaQueryWrapper<ChatAgentOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatAgentOptionApiKeyRel>()
                .eq(ChatAgentOptionApiKeyRel::getChatAgentOptionId, chatAgentOptionId);
        return selectList(qw);
    }

    default List<ChatAgentOptionApiKeyRel> findByChatApiKeyId(Long chatApiKeyId) {
        LambdaQueryWrapper<ChatAgentOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatAgentOptionApiKeyRel>()
                .eq(ChatAgentOptionApiKeyRel::getChatApiKeyId, chatApiKeyId);
        return selectList(qw);
    }

    default ChatAgentOptionApiKeyRel findByRelation(Long chatAgentOptionId, Long chatApiKeyId) {
        LambdaQueryWrapper<ChatAgentOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatAgentOptionApiKeyRel>()
                .eq(ChatAgentOptionApiKeyRel::getChatAgentOptionId, chatAgentOptionId)
                .eq(ChatAgentOptionApiKeyRel::getChatApiKeyId, chatApiKeyId);
        return selectOne(qw);
    }

    default void deleteByChatAgentOptionId(Long chatAgentOptionId) {
        LambdaQueryWrapper<ChatAgentOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatAgentOptionApiKeyRel>()
                .eq(ChatAgentOptionApiKeyRel::getChatAgentOptionId, chatAgentOptionId);
        delete(qw);
    }

    default void deleteByChatApiKeyId(Long chatApiKeyId) {
        LambdaQueryWrapper<ChatAgentOptionApiKeyRel> qw = new LambdaQueryWrapper<ChatAgentOptionApiKeyRel>()
                .eq(ChatAgentOptionApiKeyRel::getChatApiKeyId, chatApiKeyId);
        delete(qw);
    }
}
