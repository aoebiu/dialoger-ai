package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.common.validation.ModelParamValidator;
import info.mengnan.dialogerai.rag.provider.ModelParamSchemaRegistry;
import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.agent.AgentOptionRequest;
import info.mengnan.dialogerai.server.param.agent.BindableKbOption;
import info.mengnan.dialogerai.server.param.agent.BindableModelOption;
import info.mengnan.dialogerai.server.param.agent.ModelBinding;
import info.mengnan.dialogerai.server.service.AgentOptionService;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/agentOption")
@RequiredArgsConstructor
public class AgentOptionController {

    private final AgentOptionService agentOptionService;
    private final MemberService memberService;

    @GetMapping("/list")
    public R list() {
        Long memberId = StpUtil.getLoginIdAsLong();
        return R.ok(agentOptionService.listByMemberId(memberId));
    }

    @GetMapping("/bindables")
    public R bindables() {
        Long memberId = StpUtil.getLoginIdAsLong();
        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        if (ctx == null)
            return R.error(MEMBER_NOT_FOUND);
        return R.ok(agentOptionService.listBindables(memberId, ctx.ownerId(), ctx.isOwner(), ctx.teamId()));
    }

    @PostMapping("/create")
    public R create(@RequestBody AgentOptionRequest request) {
        if (request.getName() == null || request.getName().isBlank())
            return R.error(AGENT_OPTION_NAME_EMPTY);

        Long memberId = StpUtil.getLoginIdAsLong();
        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        Long ownerId = ctx.ownerId();
        if (!isModelBindingsValid(ownerId, request.getModelBindings()))
            return R.error(AGENT_OPTION_MODEL_INVALID);
        if (!isKbBindingsValid(memberId, request.getKbIds()))
            return R.error(AGENT_OPTION_KB_INVALID);

        request.setMemberId(memberId);
        request.setOwnerId(ownerId);
        return R.ok(agentOptionService.create(request));
    }

    @PutMapping("/{id}")
    public R update(@PathVariable("id") Long id, @RequestBody AgentOptionRequest request) {
        ChatAgentOption existing = agentOptionService.findById(id);
        if (existing == null)
            return R.error(AGENT_OPTION_NOT_FOUND);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberId.equals(existing.getMemberId()))
            return R.error(AGENT_OPTION_WRITE_DENIED);

        if (StringUtils.isEmpty(request.getName()))
            return R.error(AGENT_OPTION_NAME_EMPTY);

        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        Long ownerId = ctx.ownerId();
        if (!isModelBindingsValid(ownerId, request.getModelBindings()))
            return R.error(AGENT_OPTION_MODEL_INVALID);
        if (!isKbBindingsValid(memberId, request.getKbIds()))
            return R.error(AGENT_OPTION_KB_INVALID);

        request.setId(id);
        request.setMemberId(memberId);
        request.setOwnerId(ownerId);
        return R.ok(agentOptionService.update(request));
    }

    @DeleteMapping("/{id}")
    public R delete(@PathVariable("id") Long id) {
        ChatAgentOption existing = agentOptionService.findById(id);
        if (existing == null)
            return R.error(AGENT_OPTION_NOT_FOUND);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberId.equals(existing.getMemberId()))
            return R.error(AGENT_OPTION_WRITE_DENIED);

        agentOptionService.delete(id);
        return R.ok();
    }

    private boolean isModelBindingsValid(Long ownerId, Map<String, ModelBinding> modelBindings) {
        if (modelBindings.isEmpty()) return true;

        Map<String, List<BindableModelOption>> bindableModels = agentOptionService.listBindableModels(ownerId);
        return modelBindings.entrySet().stream().allMatch(entry -> {
            String keyType = entry.getKey();
            ModelBinding binding = entry.getValue();

            if (StringUtils.isEmpty(keyType) || binding == null)
                return false;

            ModelType  modelType = ModelType.valueOf(keyType.toUpperCase());
            if (StringUtils.isEmpty(binding.getModelName())) return true;

            return bindableModels.getOrDefault(keyType, List.of()).stream()
                    .filter(option -> binding.getModelName().equals(option.getModelName()))
                    .findFirst()
                    .map(option -> validateParams(option, modelType, binding.getParams()))
                    .orElse(false);
        });
    }

    private boolean validateParams(BindableModelOption option, ModelType modelType, String params) {
        if (StringUtils.isEmpty(params)) return true;
        if (!JSONUtil.isJsonObj(params)) return false;

        try {
            ModelParamValidator.validate(
                    option.getModelProvider(),
                    modelType,
                    JSONUtil.parseObj(params),
                    ModelParamSchemaRegistry.LOOKUP
            );
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isKbBindingsValid(Long memberId, List<Long> kbIds) {
        if (kbIds == null || kbIds.isEmpty()) return true;

        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        if (ctx == null) return false;
        Set<Long> bindableKbIds = agentOptionService.listBindableKnowledgeBases(memberId, ctx.isOwner(), ctx.teamId())
                .stream()
                .map(BindableKbOption::getId)
                .collect(Collectors.toSet());
        return kbIds.stream().allMatch(kbId -> kbId == null || bindableKbIds.contains(kbId));
    }
}
