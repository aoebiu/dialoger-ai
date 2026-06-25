package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberRelationMapper extends BaseMapper<ChatMemberRelation> {
}
