package info.mengnan.dialogerai.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import info.mengnan.dialogerai.rag.ChatService;
import info.mengnan.dialogerai.rag.handler.StreamingResponseHandler;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import info.mengnan.dialogerai.repository.entity.ChatMessage;
import info.mengnan.dialogerai.repository.entity.ChatSession;
import info.mengnan.dialogerai.repository.repo.ChatMessageRagSourceRepository;
import info.mengnan.dialogerai.server.core.DefaultAiServiceAssembler;
import info.mengnan.dialogerai.server.service.ChatMessageService;
import info.mengnan.dialogerai.server.service.ChatMessageToolExecutionService;
import info.mengnan.dialogerai.server.service.ChatSessionService;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.service.ModelConfigService;
import info.mengnan.dialogerai.server.param.chat.ChatRequest;
import info.mengnan.dialogerai.server.param.R;
import info.mengnan.dialogerai.server.handler.FluxStreamingResponseHandler;
import info.mengnan.dialogerai.server.param.chat.ChatConversations;
import info.mengnan.dialogerai.server.param.chat.ChatHistoryResponse;
import info.mengnan.dialogerai.server.param.chat.ChatSessionResponse;
import info.mengnan.dialogerai.server.param.chat.RagSourceDto;
import info.mengnan.dialogerai.server.param.chat.ToolExecutionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static info.mengnan.dialogerai.common.param.MessageRole.*;
import static info.mengnan.dialogerai.server.param.ErrorCode.MODEL_DEFAULT_REQUIRED;
import static info.mengnan.dialogerai.server.param.chat.ChatSessionResponse.DEFAULT_TITLE;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageToolExecutionService chatMessageToolExecutionService;
    private final ChatMessageRagSourceRepository ragSourceRepository;
    private final MemberService memberService;
    private final ModelConfigService modelConfigService;

    /**
     * 流式对话接口 - 使用 HTTP Streaming (application/x-ndjson)
     */
    @PostMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            return Flux.error(new IllegalArgumentException("sessionId 不能为空"));
        }
        if (chatSessionService.findBySessionId(sessionId) == null) {
            return Flux.error(new IllegalArgumentException("sessionId 不存在"));
        }
        Long memberId = StpUtil.getLoginIdAsLong();
        request.setMemberId(memberId);
        Long ownerId = memberService.resolveResourceOwnerId(memberId);
        String defaultModelName = modelConfigService.findDefaultDirectChatModelName(ownerId);
        if (defaultModelName == null || defaultModelName.isBlank())
            return Flux.error(new IllegalArgumentException(MODEL_DEFAULT_REQUIRED.getMessage()));
        return streamResponse(request);
    }

    @GetMapping(value = "/createChat")
    public R createChat() {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatSession session = chatSessionService.createChat(memberId);
        if (session == null) {
            return R.ok();
        }
        return R.ok(new ChatSessionResponse(session.getChatSessionId(), session.getTitle(), session.getUpdatedAt()));
    }

    // 展示在页面上的增量数据
    @GetMapping(value = "/conversations")
    public R conversations(@RequestParam("sessionId") String sessionId) {
        Long memberId = StpUtil.getLoginIdAsLong();
        ChatConversations chatConversations = new ChatConversations(memberId,sessionId);

        ChatSession chatSession = chatSessionService.findBySessionId(sessionId);
        if (chatSession != null && !DEFAULT_TITLE.equals(chatSession.getTitle())) {
            chatConversations.setTitle(chatSession.getTitle());
        }
        ChatMessage latest = chatMessageService.findLatest(sessionId, ASSISTANT.n());
        if (latest != null) {
            List<Long> sourceIds = ragSourceRepository.findIdsForMessage(latest.getId());
            if (!sourceIds.isEmpty()) chatConversations.setSourceIds(sourceIds);
            List<Long> toolExecutionIds = chatMessageToolExecutionService.findIdsForMessage(latest.getId());
            if (!toolExecutionIds.isEmpty()) chatConversations.setToolExecutionIds(toolExecutionIds);
        }

        return R.ok(chatConversations);
    }


    /**
     * 流式响应 - 返回纯文本流
     * 使用回调接口将 ChatService 的响应转换为 Flux
     */
    private Flux<String> streamResponse(ChatRequest chatRequest) {
        return Flux.create(sink -> {
            try {
                // todo llm正在生成的时候不能删除(页面强制刷新后重新调用)
                if (chatRequest.getFromMessageId() != null)
                    chatMessageService.truncateMessages(chatRequest.getSessionId(), chatRequest.getFromMessageId());

                StreamingResponseHandler handler = new FluxStreamingResponseHandler(sink, chatRequest.getSessionId());
                chatService.chatStreaming(
                        chatRequest.getSessionId(),
                        chatRequest.getMessage(),
                        handler,
                        new DefaultAiServiceAssembler(
                                chatRequest.getMemberId(),
                                chatRequest.getOptionId()));
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * 展示在对话列表的所有数据
     */
    @GetMapping(value = "/history/{sessionId}")
    public R history(@PathVariable("sessionId") String sessionId) {
        List<ChatMessage> list = chatMessageService.findChat(sessionId,
                List.of(ASSISTANT.n(), SYSTEM.n(), USER.n()));
        // todo 判断是否有,再查ragSourceMap或toolExecutionMap
        Map<Long, List<Long>> ragSourceMap = chatMessageService.findRagSourceIdMap(sessionId);
        Map<Long, List<Long>> toolExecutionMap = chatMessageToolExecutionService.findToolExecutionIdMap(sessionId);
        return R.ok(new ChatHistoryResponse(list, ragSourceMap, toolExecutionMap));
    }


    /**
     * 批量获取知识库片段
     */
    @GetMapping("/ragSources")
    public R getRagSources(@RequestParam("ids") List<Long> ids) {
        List<RagSourceDto> sources = chatMessageService.findRagSources(ids).stream().map(RagSourceDto::from).toList();
        return R.ok(Map.of("sources", sources));
    }

    /**
     * 获取指定工具调用记录
     */
    @GetMapping("/toolExecutions")
    public R getToolExecutions(@RequestParam("ids") List<Long> ids) {
        List<ToolExecutionDto> executions = chatMessageToolExecutionService.findToolExecutions(ids).stream()
                .map(ToolExecutionDto::from)
                .toList();
        return R.ok(Map.of("executions", executions));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public R clearHistory(@PathVariable(name = "sessionId") String sessionId) {
        chatMessageService.truncateMessages(sessionId,0L);
        chatSessionService.deleteBySessionId(sessionId);
        return R.ok();
    }

    /**
     * 获取所有对话会话
     */
    @GetMapping(value = "/sessions")
    public R getAllSessions() {
        Long memberId = StpUtil.getLoginIdAsLong();
        List<ChatSession> sessions = chatSessionService.findAll(memberId);
        List<ChatSessionResponse> responses = sessions.stream()
                .map(session -> new ChatSessionResponse(session.getChatSessionId(), session.getTitle(), session.getUpdatedAt()))
                .toList();
        return R.ok(responses);
    }
}
