package info.mengnan.dialogerai.repository.repo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final MemberMapper mapper;

    public ChatMember findById(Long id) {
        return mapper.selectById(id);
    }

    public List<ChatMember> findByIds(List<Long> ids) {
        Map<Long, ChatMember> memberMap = mapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ChatMember::getId, Function.identity()));
        return ids.stream()
                .map(memberMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public ChatMember findByUsername(String username) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatMember>()
                .eq(ChatMember::getUsername, username));
    }

    public ChatMember findByPhone(String phone) {
        return mapper.selectOne(new LambdaQueryWrapper<ChatMember>()
                .eq(ChatMember::getPhone, phone));
    }

    public void insert(ChatMember entity) {
        mapper.insert(entity);
    }

    public void updateById(ChatMember entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public List<ChatMember> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<ChatMember>()
                .orderByDesc(ChatMember::getId));
    }
}
