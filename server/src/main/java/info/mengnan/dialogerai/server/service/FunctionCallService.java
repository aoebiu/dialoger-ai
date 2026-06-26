package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatToolDescription;
import info.mengnan.dialogerai.repository.repo.MemberRepository;
import info.mengnan.dialogerai.repository.repo.ToolDescriptionRepository;
import info.mengnan.dialogerai.server.param.functionCall.FunctionCallRequest;
import info.mengnan.dialogerai.server.param.functionCall.FunctionCallResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallService {

    private final ToolDescriptionRepository toolDescriptionRepository;
    private final MemberRepository memberRepository;

    public ChatToolDescription findById(Long id) {
        return toolDescriptionRepository.findById(id);
    }

    public ChatToolDescription findByNameAndMemberId(String name, Long memberId) {
        return toolDescriptionRepository.findByNameAndMemberId(name, memberId);
    }

    public List<FunctionCallResponse> list(List<Long> teamMemberIds) {
        if (teamMemberIds == null || teamMemberIds.isEmpty())
            return List.of();

        List<ChatToolDescription> tools = toolDescriptionRepository.findByMemberIds(teamMemberIds);
        List<Long> memberIds = tools.stream().map(ChatToolDescription::getMemberId).distinct().toList();
        Map<Long, String> memberNameMap = memberRepository.findByIds(memberIds).stream()
                .collect(Collectors.toMap(ChatMember::getId, ChatMember::getUsername));
        return tools.stream()
                .map(t -> FunctionCallResponse.from(t, memberNameMap.get(t.getMemberId())))
                .toList();
    }

    public ChatToolDescription create(Long memberId, FunctionCallRequest request) {
        ChatToolDescription entity = new ChatToolDescription();
        entity.setMemberId(memberId);
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setProperty(request.getProperty());
        entity.setRequired(request.getRequired());
        entity.setExecute(request.getExecute());
        entity.setGeneratePrompt(request.getGeneratePrompt());
        toolDescriptionRepository.insert(entity);
        log.info("function call tool created: memberId={}, id={}, name={}", memberId, entity.getId(), entity.getName());
        return entity;
    }

    public ChatToolDescription update(Long id, FunctionCallRequest request) {
        ChatToolDescription update = new ChatToolDescription();
        update.setId(id);
        update.setName(request.getName().trim());
        update.setDescription(request.getDescription());
        update.setProperty(request.getProperty());
        update.setRequired(request.getRequired());
        update.setExecute(request.getExecute());
        update.setGeneratePrompt(request.getGeneratePrompt());
        toolDescriptionRepository.updateById(update);
        return findById(id);
    }

    public void delete(Long id) {
        toolDescriptionRepository.deleteById(id);
    }
}
