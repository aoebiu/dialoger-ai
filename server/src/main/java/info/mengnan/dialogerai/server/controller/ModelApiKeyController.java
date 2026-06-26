package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.repository.repo.ChatApiKeyRepository;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.param.apiKey.ModelApiKeyResponse;
import info.mengnan.dialogerai.server.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型 API Key 控制器
 * 用于管理外部模型的 API Key（chat_api_key 表）
 * 区别于 chat_project_api_key（本项目创建的 API Key）
 */
@Slf4j
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelApiKeyController {

    private final ChatApiKeyRepository chatApiKeyService;
    private final MemberService memberService;

    /**
     * 获取当前团队的模型列表（OWNER 查自己的，MEMBER 查关联 OWNER 的）
     */
    @GetMapping("/list")
    public R listModels() {
        Long memberId = StpUtil.getLoginIdAsLong();
        Long ownerId = memberService.resolveResourceOwnerId(memberId);
        List<ChatApiKey> keys = chatApiKeyService.findAll(ownerId);
        List<ModelApiKeyResponse> list = keys.stream().map(k -> {
            ModelApiKeyResponse response = new ModelApiKeyResponse();
            response.setId(k.getId());
            response.setModelName(k.getModelName());
            response.setModelProvider(k.getModelProvider());
            response.setKeyType(k.getKeyType());
            response.setMaskedApiKey(maskApiKey(k.getApiKey()));
            response.setCreatedAt(k.getCreatedAt());
            return response;
        }).toList();
        return R.ok(list);
    }

    /**
     * 创建新的模型 API Key（仅 OWNER 可操作）
     */
    @PostMapping("/create")
    public R createApiKey(@RequestParam(name = "modelName") String modelName,
                          @RequestParam(name = "modelProvider") String modelProvider,
                          @RequestParam(name = "keyType") String keyType,
                          @RequestParam(name = "apiKey") String apiKey,
                          @RequestParam(name = "param", required = false) String param) {

        if (param != null && !param.isBlank() && !JSONUtil.isJsonObj(param)) {
            return R.error("param 必须是合法的 JSON 对象");
        }

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId)) {
            return R.error("仅 Owner 可执行此操作");
        }

        ChatApiKey entity = new ChatApiKey();
        entity.setMemberId(memberId);
        entity.setModelName(modelName);
        entity.setModelProvider(modelProvider);
        entity.setKeyType(keyType);
        entity.setApiKey(apiKey);

        chatApiKeyService.insert(entity);
        log.info("User {} created Model API Key: {} for model {}", memberId, entity.getId(), modelName);

        ModelApiKeyResponse response = new ModelApiKeyResponse();
        response.setId(entity.getId());
        response.setModelName(entity.getModelName());
        response.setModelProvider(entity.getModelProvider());
        response.setKeyType(entity.getKeyType());
        response.setCreatedAt(entity.getCreatedAt());
        return R.ok(response);
    }

    /**
     * 删除模型 API Key（仅 OWNER 可操作，且只能删除本团队的 Key）
     */
    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId)) {
            return R.error("仅 Owner 可执行此操作");
        }

        ChatApiKey chatApiKey = chatApiKeyService.findById(id);
        List<Long> teamMemberIds = memberService.resolveTeamMemberIds(memberId);
        if (chatApiKey == null || !teamMemberIds.contains(chatApiKey.getMemberId())) {
            return R.error("该 API Key 无法删除");
        }

        chatApiKeyService.deleteById(id);
        return R.ok();
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}