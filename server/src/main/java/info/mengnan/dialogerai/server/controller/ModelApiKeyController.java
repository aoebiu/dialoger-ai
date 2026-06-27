package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.util.JSONUtil;
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
                          @RequestParam(name = "apiKey") String apiKey,
                          @RequestParam(name = "param", required = false) String param) {

        if (modelName == null || modelName.isBlank()
                || modelProvider == null || modelProvider.isBlank()
                || keyType == null || keyType.isBlank()
                || apiKey == null || apiKey.isBlank()
                || (param != null && !param.isBlank() && !JSONUtil.isJsonObj(param)))
            return R.error(MODEL_PARAM_INVALID);

        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId))
            return R.error(MEMBER_OWNER_REQUIRED);

        return R.ok(modelApiKeyService.create(memberId, modelName, modelProvider, keyType, apiKey));
    }

    @PutMapping("/{id}/directChat")
    public R setDefaultDirectChatModel(@PathVariable(name = "id") Long id) {
        Long memberId = StpUtil.getLoginIdAsLong();
        if (!memberService.isOwner(memberId))
            return R.error(MEMBER_OWNER_REQUIRED);

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
            return R.error(MEMBER_OWNER_REQUIRED);

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
            return R.error(MEMBER_OWNER_REQUIRED);

        ChatApiKey chatApiKey = modelApiKeyService.findById(id);
        if (chatApiKey == null)
            return R.error(MODEL_KEY_NOT_FOUND);
        if (!memberService.resolveTeamMemberIds(memberId).contains(chatApiKey.getMemberId()))
            return R.error(MODEL_KEY_DELETE_DENIED);

        modelApiKeyService.delete(id);
        return R.ok();
    }
}
