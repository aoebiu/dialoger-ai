package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.rag.provider.ModelParamSchemaRegistry;
import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
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
        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        return R.ok(modelApiKeyService.list(ctx.ownerId()));
    }

    @PostMapping("/create")
    public R createApiKey(@RequestParam(name = "modelName") String modelName,
                          @RequestParam(name = "modelProvider") String modelProvider,
                          @RequestParam(name = "keyType") String keyType,
                          @RequestParam(name = "apiKey") String apiKey,
                          @RequestParam(name = "description", required = false) String description) {
        Long memberId = StpUtil.getLoginIdAsLong();
        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        if (ctx == null || !ctx.isOwner())
            return R.error(MEMBER_MANAGE_DENIED);

        return R.ok(modelApiKeyService.create(ctx.ownerId(), modelName, modelProvider, keyType, apiKey, description));
    }

    @DeleteMapping("/{id}")
    public R deleteApiKey(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        if (ctx == null || !ctx.isOwner())
            return R.error(MEMBER_MANAGE_DENIED);

        ChatApiKey chatApiKey = modelApiKeyService.findById(id);
        if (chatApiKey == null)
            return R.error(MODEL_KEY_NOT_FOUND);
        if (!memberService.isTeamMember(ctx, chatApiKey.getMemberId()))
            return R.error(MODEL_KEY_DELETE_DENIED);
        if (modelApiKeyService.isBound(id))
            return R.error(MODEL_KEY_BOUND);

        modelApiKeyService.delete(ctx.ownerId(), id);
        return R.ok();
    }

    @PutMapping("/{id}/toggle")
    public R toggleEnabled(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        MemberTeamContext ctx = memberService.resolveTeamContext(memberId);
        if (ctx == null || !ctx.isOwner())
            return R.error(MEMBER_MANAGE_DENIED);

        ChatApiKey chatApiKey = modelApiKeyService.findById(id);
        if (chatApiKey == null)
            return R.error(MODEL_KEY_NOT_FOUND);
        if (!memberService.isTeamMember(ctx, chatApiKey.getMemberId()))
            return R.error(MODEL_KEY_DELETE_DENIED);

        boolean isDisabling = Boolean.TRUE.equals(chatApiKey.getEnabled());
        if (isDisabling && modelApiKeyService.isBound(id))
            return R.error(MODEL_KEY_BOUND);

        return R.ok(modelApiKeyService.toggleEnabled(id));
    }
}
