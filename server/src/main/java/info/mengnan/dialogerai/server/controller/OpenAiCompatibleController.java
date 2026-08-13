package info.mengnan.dialogerai.server.controller;

import info.mengnan.dialogerai.common.param.ModelCapability;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.ChatService;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.handler.StreamingResponseHandler;
import info.mengnan.dialogerai.repository.entity.ChatProjectApiKey;
import info.mengnan.dialogerai.repository.repo.ProjectApiKeyRepository;
import info.mengnan.dialogerai.server.core.DefaultAiServiceAssembler;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.handler.OpenAiStreamingResponseHandler;
import info.mengnan.dialogerai.server.param.chat.ChatRequest;
import info.mengnan.dialogerai.server.param.openai.OpenApiChatRequest;
import info.mengnan.dialogerai.server.service.ImageProcessingService;
import info.mengnan.dialogerai.server.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static info.mengnan.dialogerai.rag.config.DefaultModelConfig.DEFAULT_OPTION_ID;
import static info.mengnan.dialogerai.rag.config.DefaultModelConfig.DEFAULT_SESSION;
import static info.mengnan.dialogerai.server.param.ErrorCode.OPENAI_AGENT_VISION_CHAT_MODEL_MISSING;
import static info.mengnan.dialogerai.server.param.ErrorCode.OPENAI_API_KEY_UNBOUND_AGENT;
import static info.mengnan.dialogerai.server.param.ErrorCode.OPENAI_MESSAGES_EMPTY;
import static info.mengnan.dialogerai.server.param.ErrorCode.OPENAI_STREAM_ONLY;

/**
 * OpenAI 兼容的 API 控制器
 * 提供标准的 OpenAI API 格式接口,可以通过本接口从第三方客户端执行
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiCompatibleController {

    private final ChatService chatService;
    private final ProjectApiKeyRepository projectApiKeyService;
    private final ImageProcessingService imageProcessingService;
    private final ModelConfigService modelConfigService;

    /**
     * OpenAI 兼容的聊天接口
     * 鉴权说明：
     * - 此接口通过 OpenAiApiKeyInterceptor 进行鉴权
     * - 需要在 Authorization header 中提供 sk- 开头的 API Key
     * - 格式：Authorization: sk-xxx
     */
    @PostMapping(value = "/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatCompletions(@RequestBody OpenApiChatRequest request,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {

        String apiKey = authorization.replace("Bearer ", "").trim();
        if (!request.getStream()) {
            return Flux.error(new BusinessException(OPENAI_STREAM_ONLY));
        }

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return Flux.error(new BusinessException(OPENAI_MESSAGES_EMPTY));
        }

        ChatProjectApiKey projectApiKey = projectApiKeyService.findByApiKey(apiKey);
        boolean hasImageContent = messagesHaveImage(request);
        ModelConfig imageModelConfig = null;
        if (hasImageContent) {
            if (projectApiKey.getChatAgentOptionId() == null) {
                return Flux.error(new BusinessException(OPENAI_API_KEY_UNBOUND_AGENT));
            }
            Map<ModelType, ModelConfig> configs =
                    modelConfigService.loadModelConfigsByAgentOptionId(projectApiKey.getChatAgentOptionId());
            imageModelConfig = configs.get(ModelType.CHAT);
            if (imageModelConfig == null || !ModelCapability.contains(imageModelConfig.getCapabilities(), ModelCapability.VISION)) {
                return Flux.error(new BusinessException(OPENAI_AGENT_VISION_CHAT_MODEL_MISSING,
                        OPENAI_AGENT_VISION_CHAT_MODEL_MISSING.getMessage()
                                + ", agentOptionId=" + projectApiKey.getChatAgentOptionId()));
            }
        }

        ChatRequest chatRequest = buildInternalRequest(request, imageModelConfig);
        chatRequest.setMemberId(projectApiKey.getMemberId());

        try {
            return streamResponse(chatRequest, request.getModel());
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private boolean messagesHaveImage(OpenApiChatRequest request) {
        if (request.getMessages() == null) return false;
        for (OpenApiChatRequest.Message msg : request.getMessages()) {
            if (CollectionUtils.isEmpty(msg.getContentParts())) continue;
            for (OpenApiChatRequest.Message.ContentPart part : msg.getContentParts()) {
                if (part.getImageUrl() != null && StringUtils.isNotEmpty(part.getImageUrl().getUrl())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 流式响应 - 返回 SSE 格式
     * 使用回调接口将 ChatService 的响应转换为 Flux
     */
    private Flux<String> streamResponse(ChatRequest chatRequest, String model) {
        String requestId = "chatcmpl-" + UUID.randomUUID();
        long timestamp = System.currentTimeMillis() / 1000;

        return Flux.<String>create(sink -> {
                    try {
                        StreamingResponseHandler handler = new OpenAiStreamingResponseHandler(
                                sink, requestId, timestamp, model);

                        chatService.chatStreaming(chatRequest.getSessionId(),
                                chatRequest.getMessage(),
                                handler,
                                new DefaultAiServiceAssembler(
                                        chatRequest.getMemberId(),
                                        chatRequest.getOptionId()));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                })
                .delayElements(Duration.ofMillis(1));
    }

    /**
     * 将 OpenAI 请求转换为内部请求格式
     */
    private ChatRequest buildInternalRequest(OpenApiChatRequest openAiRequest, ModelConfig imageModelConfig) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setInDB(false);
        chatRequest.setSessionId(DEFAULT_SESSION);
        chatRequest.setOptionId(DEFAULT_OPTION_ID);

        // 将所有消息按照角色和内容组合成一个字符串
        StringBuilder messageBuilder = new StringBuilder();
        for (OpenApiChatRequest.Message msg : openAiRequest.getMessages()) {
            // 优先使用多模态内容，如果不存在则使用传统内容
            if (CollectionUtils.isNotEmpty(msg.getContentParts())) {
                for (OpenApiChatRequest.Message.ContentPart part : msg.getContentParts()) {
                    if ("text".equals(part.getType()) && part.getText() != null) {
                        messageBuilder.append(part.getText()).append(" ");
                    } else if (part.getImageUrl() != null) {
                        String imageUrl = part.getImageUrl().getUrl();
                        if (StringUtils.isNotEmpty(imageUrl)) {
                            String imageDescription = imageProcessingService.processImageUrl(imageUrl, imageModelConfig);
                            messageBuilder.append(imageDescription).append(" ");
                        }
                    }
                }
            } else if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                // 回退到传统文本内容
                messageBuilder.append(msg.getContent()).append("\n");
            }
        }

        chatRequest.setMessage(messageBuilder.toString().trim());
        return chatRequest;
    }
}
