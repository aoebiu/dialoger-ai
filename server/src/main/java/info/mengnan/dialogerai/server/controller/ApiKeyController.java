package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.repository.entity.ChatAgentOption;
import info.mengnan.dialogerai.repository.entity.ChatProjectApiKey;
import info.mengnan.dialogerai.repository.repo.ChatAgentOptionRepository;
import info.mengnan.dialogerai.repository.repo.ProjectApiKeyRepository;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.apiKey.ApiKeyCreateRequest;
import info.mengnan.dialogerai.server.param.apiKey.ApiKeyUpdateRequest;
import info.mengnan.dialogerai.server.param.apiKey.ProjectApiKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API Key 管理控制器
 * 管理本项目创建的 API Key（chat_project_api_key 表）
 * 区别于 chat_model_api_key（外部模型的 API Key）
 */
@Slf4j
@RestController
@RequestMapping("/api/apikey")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ProjectApiKeyRepository projectApiKeyService;
    private final ChatAgentOptionRepository chatAgentOptionRepository;

    /**
     * 获取当前用户的 API Key 列表（列表中 key 脱敏显示）
     */
    @GetMapping("/list")
    public R listApiKeys() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatProjectApiKey> keys = projectApiKeyService.listByMemberId(memberId);
        Map<Long, String> agentNames = loadAgentNames(memberId);

        List<ProjectApiKeyResponse> list = keys.stream().map(k -> toResponse(k, agentNames)).toList();
        return R.ok(list);
    }

    private Map<Long, String> loadAgentNames(Long memberId) {
        List<ChatAgentOption> agents = chatAgentOptionRepository.findByMemberId(memberId);
        return agents.stream().collect(Collectors.toMap(ChatAgentOption::getId, ChatAgentOption::getName, (a, b) -> a));
    }

    private ProjectApiKeyResponse toResponse(ChatProjectApiKey k, Map<Long, String> agentNames) {
        ProjectApiKeyResponse response = new ProjectApiKeyResponse();
        response.setId(k.getId());
        response.setName(k.getName());
        response.setStatus(k.getStatus());
        response.setExpiresAt(k.getExpiresAt());
        response.setLastUsedAt(k.getLastUsedAt());
        response.setCreatedAt(k.getCreatedAt());
        response.setApiKey(maskKey(k.getApiKey()));
        response.setChatAgentOptionId(k.getChatAgentOptionId());
        if (k.getChatAgentOptionId() != null) {
            response.setChatAgentOptionName(agentNames.get(k.getChatAgentOptionId()));
        }
        return response;
    }

    private static String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "sk-****";
        return "sk-****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 创建新的 API Key
     */
    @PostMapping("/create")
    public R createApiKey(@RequestBody(required = false) ApiKeyCreateRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();
        if (request == null) request = new ApiKeyCreateRequest();

        Long agentOptionId = request.getChatAgentOptionId();
        if (agentOptionId != null && !isOwnedAgent(memberId, agentOptionId)) {
            return R.error("绑定的 Agent 不存在或无权访问");
        }

        String apiKey = "sk-" + UUID.randomUUID().toString().replace("-", "");

        ChatProjectApiKey entity = new ChatProjectApiKey();
        entity.setApiKey(apiKey);
        entity.setMemberId(memberId);
        entity.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
        entity.setChatAgentOptionId(agentOptionId);
        entity.setStatus(1);

        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            entity.setExpiresAt(LocalDateTime.now().plusDays(request.getExpiresInDays()));
        }
        projectApiKeyService.insert(entity);
        log.info("User {} created API Key: {}, agentOptionId={}", memberId, entity.getId(), agentOptionId);

        ProjectApiKeyResponse response = new ProjectApiKeyResponse();
        response.setId(entity.getId());
        response.setApiKey(apiKey);
        response.setName(entity.getName());
        response.setExpiresAt(entity.getExpiresAt());
        response.setChatAgentOptionId(entity.getChatAgentOptionId());
        if (entity.getChatAgentOptionId() != null) {
            ChatAgentOption agent = chatAgentOptionRepository.findById(entity.getChatAgentOptionId());
            if (agent != null) response.setChatAgentOptionName(agent.getName());
        }
        return R.ok(response);
    }

    /**
     * 更新 API Key 的 Agent 绑定
     */
    @PutMapping("/{id}")
    public R updateApiKey(@PathVariable("id") Long id, @RequestBody ApiKeyUpdateRequest request) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = projectApiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 不存在或无权修改");
        }

        Long agentOptionId = request.getChatAgentOptionId();
        if (agentOptionId != null && !isOwnedAgent(memberId, agentOptionId)) {
            return R.error("绑定的 Agent 不存在或无权访问");
        }

        projectApiKey.setChatAgentOptionId(agentOptionId);
        projectApiKeyService.updateById(projectApiKey);
        return R.ok();
    }

    private boolean isOwnedAgent(Long memberId, Long agentOptionId) {
        ChatAgentOption agent = chatAgentOptionRepository.findById(agentOptionId);
        return agent != null && memberId.equals(agent.getMemberId());
    }

    /**
     * 禁用 API Key
     */
    @PostMapping("/disable/{id}")
    public R disableApiKey(@PathVariable("id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = projectApiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }

        projectApiKey.setStatus(0);
        projectApiKeyService.updateById(projectApiKey);
        return R.ok();
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable("id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();

        ChatProjectApiKey projectApiKey = projectApiKeyService.findById(id);
        if (projectApiKey == null || !memberId.equals(projectApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }
        projectApiKeyService.deleteById(id);
        return R.ok();
    }
}
