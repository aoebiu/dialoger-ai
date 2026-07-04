package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.provider.ModelParamSchemaRegistry;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.service.ModelApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static info.mengnan.dialogerai.server.param.ErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelApiKeyController {

    private final ModelApiKeyService modelApiKeyService;
    private final MemberService memberService;

    /**
     * 获取全部 Provider × 模型类型的参数 Schema（字段名、类型、范围、默认值）。
     * 前端应在应用内缓存结果，仅在缓存缺失时调用一次。
     */
    @GetMapping("/schema")
    public R getParamSchema() {
        return R.ok(ModelParamSchemaRegistry.listAllSchemas());
    }

    @GetMapping("/list")
    public R listModels() {
        Long memberId = StpUtil.getLoginIdAsLong();
        Long ownerId = memberService.resolveResourceOwnerId(memberId);
        return R.ok(modelApiKeyService.list(ownerId));
    }

    @PostMapping("/create")
    public R createApiKey(@RequestParam(name = "modelName") String modelName,
                          @RequestParam(name = "modelProvider") String modelProvider,
                          @RequestParam(name = "keyType") String keyType,
                          @RequestParam(name = "apiKey") String apiKey) {

        if (modelName == null || modelName.isBlank()
                || modelProvider == null || modelProvider.isBlank()
                || keyType == null || keyType.isBlank()
                || apiKey == null || apiKey.isBlank())
            return R.error(MODEL_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId))
            return R.error(MEMBER_MANAGE_DENIED);

        return R.ok(modelApiKeyService.create(memberId, modelName, modelProvider, keyType, apiKey));
    }

    @PutMapping("/{id}/directChat")
    public R setDefaultDirectChatModel(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId))
            return R.error(MEMBER_MANAGE_DENIED);

        ChatApiKey chatApiKey = modelApiKeyService.findById(id);
        if (chatApiKey == null)
            return R.error(MODEL_KEY_NOT_FOUND);
        if (!ModelType.CHAT.n().equals(chatApiKey.getKeyType()))
            return R.error(MODEL_DEFAULT_INVALID);

        Long ownerId = memberService.resolveResourceOwnerId(memberId);
        if (!ownerId.equals(chatApiKey.getMemberId()))
            return R.error(MODEL_KEY_NOT_FOUND);
        return R.ok(modelApiKeyService.setDefaultDirectChatModel(ownerId, id));
    }

    @DeleteMapping("/{id}/directChat")
    public R clearDefaultDirectChatModel(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId))
            return R.error(MEMBER_MANAGE_DENIED);

        ChatApiKey chatApiKey = modelApiKeyService.findById(id);
        if (chatApiKey == null)
            return R.error(MODEL_KEY_NOT_FOUND);
        if (!chatApiKey.isDefaultChat())
            return R.error(MODEL_DEFAULT_INVALID);

        Long ownerId = memberService.resolveResourceOwnerId(memberId);
        if (!ownerId.equals(chatApiKey.getMemberId()))
            return R.error(MODEL_KEY_NOT_FOUND);
        return R.ok(modelApiKeyService.clearDefaultDirectChatModel(ownerId, id));
    }

    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId))
            return R.error(MEMBER_MANAGE_DENIED);

        ChatApiKey chatApiKey = modelApiKeyService.findById(id);
        if (chatApiKey == null)
            return R.error(MODEL_KEY_NOT_FOUND);
        if (!memberService.isTeamMember(memberService.resolveTeamId(memberId), chatApiKey.getMemberId()))
            return R.error(MODEL_KEY_DELETE_DENIED);

        modelApiKeyService.delete(id);
        return R.ok();
    }
}
