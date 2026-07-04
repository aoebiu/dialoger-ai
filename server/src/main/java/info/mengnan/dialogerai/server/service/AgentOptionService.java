package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver.KbIndexRef;
import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import info.mengnan.dialogerai.repository.entity.ChatAgentOptionApiKeyRel;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionApiKeyRelRepository;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionRepository;
import info.mengnan.dialogerai.repository.repo.ChatApiKeyRepository;
import info.mengnan.dialogerai.server.param.agent.AgentOptionBindablesResponse;
import info.mengnan.dialogerai.server.param.agent.AgentOptionRequest;
import info.mengnan.dialogerai.server.param.agent.AgentOptionResponse;
import info.mengnan.dialogerai.server.param.agent.BindableKbOption;
import info.mengnan.dialogerai.server.param.agent.BindableModelOption;
import info.mengnan.dialogerai.server.param.agent.ModelBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOptionService {

    private final ChatAgentOptionRepository chatAgentOptionRepository;
    private final ChatAgentOptionApiKeyRelRepository chatAgentOptionApiKeyRelRepository;
    private final ChatApiKeyRepository chatApiKeyRepository;
    private final KnowledgeBaseService knowledgeBaseService;

    public ChatAgentOption findById(Long id) {
        return chatAgentOptionRepository.findById(id);
    }

    public List<AgentOptionResponse> listByMemberId(Long memberId) {
        return chatAgentOptionRepository.findByMemberId(memberId).stream()
                .map(option -> buildAgentOptionResponse(option.getId()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentOptionResponse create(AgentOptionRequest request) {
        ChatAgentOption option = buildChatAgentOption(request);
        chatAgentOptionRepository.insert(option);
        saveModelBindings(option.getId(), request);
        log.info("agent option created: memberId={}, id={}, name={}", request.getMemberId(), option.getId(), option.getName());
        return buildAgentOptionResponse(option.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentOptionResponse update(AgentOptionRequest request) {
        ChatAgentOption option = buildChatAgentOption(request);
        chatAgentOptionRepository.update(option);

        saveModelBindings(option.getId(), request);
        log.info("agent option updated: memberId={}, id={}", request.getMemberId(), option.getId());
        return buildAgentOptionResponse(option.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        chatAgentOptionApiKeyRelRepository.deleteByChatAgentOptionId(id);
        chatAgentOptionRepository.deleteById(id);
        log.info("agent option deleted: id={}", id);
    }

    public Map<String, List<BindableModelOption>> listBindableModels(Long ownerId) {
        Map<String, List<BindableModelOption>> fromDb = chatApiKeyRepository.findByMemberId(ownerId).stream()
                .collect(Collectors.groupingBy(
                        ChatApiKey::getKeyType,
                        Collectors.mapping(
                                apiKey -> new BindableModelOption(apiKey.getModelName(), apiKey.getModelProvider()),
                                Collectors.toList())));

        return Arrays.stream(ModelType.values())
                .collect(Collectors.toMap(
                        ModelType::n,
                        type -> fromDb.getOrDefault(type.n(), List.of()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public List<BindableKbOption> listBindableKnowledgeBases(Long memberId, boolean isOwner, List<Long> teamMemberIds) {
        return knowledgeBaseService.listVisibleActive(memberId, isOwner, teamMemberIds).stream()
                .map(BindableKbOption::new)
                .toList();
    }

    public AgentOptionBindablesResponse listBindables(Long memberId, Long ownerId, boolean isOwner, List<Long> teamMemberIds) {
        return new AgentOptionBindablesResponse(
                listBindableModels(ownerId),
                listBindableKnowledgeBases(memberId, isOwner, teamMemberIds));
    }

    public List<Long> findBoundKbIds(Long agentOptionId) {
        ChatAgentOption option = chatAgentOptionRepository.findById(agentOptionId);
        if (option == null || option.getKbIds() == null)
            return List.of();
        return option.getKbIds();
    }

    public List<KbIndexRef> resolveBoundKbIndexRefs(Long agentOptionId) {
        return knowledgeBaseService.resolveActiveKbIndexRefs(findBoundKbIds(agentOptionId));
    }

    private AgentOptionResponse buildAgentOptionResponse(Long optionId) {
        Map<String, ModelBinding> bindingMap = loadModelBindings(optionId);
        ChatAgentOption option = chatAgentOptionRepository.findById(optionId);
        return new AgentOptionResponse(option, bindingMap);
    }

    private Map<String, ModelBinding> loadModelBindings(Long agentOptionId) {
        List<ChatAgentOptionApiKeyRel> rels = chatAgentOptionApiKeyRelRepository.findByChatAgentOptionId(agentOptionId);
        if (CollectionUtils.isEmpty(rels))
            return new LinkedHashMap<>();

        Map<Long, ChatApiKey> apiKeyMap = chatApiKeyRepository.findByIds(
                rels.stream().map(ChatAgentOptionApiKeyRel::getChatApiKeyId).toList()).stream()
                .collect(Collectors.toMap(ChatApiKey::getId, key -> key));

        return rels.stream()
                .map(rel -> {
                    ChatApiKey apiKey = apiKeyMap.get(rel.getChatApiKeyId());
                    if (apiKey == null) return null;
                    return Map.entry(apiKey.getKeyType(), new ModelBinding(apiKey.getModelName(), rel.getParams()));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right, LinkedHashMap::new));
    }

    private void saveModelBindings(Long agentOptionId, AgentOptionRequest request) {
        chatAgentOptionApiKeyRelRepository.deleteByChatAgentOptionId(agentOptionId);
        if (MapUtils.isEmpty(request.getModelBindings())) return;

        request.getModelBindings().entrySet().stream()
                .filter(entry -> entry.getValue() != null && StringUtils.isNotBlank(entry.getValue().getModelName()))
                .forEach(entry -> {
                    ModelBinding binding = entry.getValue();
                    ChatApiKey apiKey = chatApiKeyRepository.findByMemberIdAndKeyTypeAndModelName(
                            request.getOwnerId(), entry.getKey(), binding.getModelName().trim());
                    ChatAgentOptionApiKeyRel rel = new ChatAgentOptionApiKeyRel();
                    rel.setChatAgentOptionId(agentOptionId);
                    rel.setChatApiKeyId(apiKey.getId());
                    rel.setParams(StringUtils.trimToNull(binding.getParams()));
                    chatAgentOptionApiKeyRelRepository.insert(rel);
                });
    }

    private ChatAgentOption buildChatAgentOption(AgentOptionRequest request) {
        ChatAgentOption option = new ChatAgentOption();
        option.setId(request.getId());
        option.setMemberId(request.getMemberId());
        option.setName(request.getName().trim());
        option.setMaxMessages(request.getMaxMessages());
        option.setEnabled(request.getEnabled());
        option.setRag(request.getRag());
        option.setTransform(request.getTransform());
        option.setContentAggregator(request.getContentAggregator());
        option.setTools(request.getTools());
        option.setContentInjectorPrompt(request.getContentInjectorPrompt());
        option.setSystemPrompt(StringUtils.trimToNull(request.getSystemPrompt()));
        option.setInDB(request.getInDB());
        option.setRemark(request.getRemark());

        List<Long> kbIds = request.getKbIds();
        if (CollectionUtils.isEmpty(kbIds))
            option.setKbIds(List.of());
        else
            option.setKbIds(kbIds.stream().filter(Objects::nonNull).distinct().toList());
        return option;
    }

}
