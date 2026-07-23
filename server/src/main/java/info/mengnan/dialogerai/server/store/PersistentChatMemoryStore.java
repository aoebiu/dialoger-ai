package info.mengnan.dialogerai.server.store;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import info.mengnan.dialogerai.repository.entity.ChatMessage;
import info.mengnan.dialogerai.repository.entity.ChatMessageExtras;
import info.mengnan.dialogerai.repository.entity.ChatSession;
import info.mengnan.dialogerai.repository.repo.ChatMessageRepository;
import info.mengnan.dialogerai.rag.injector.RagSourceStore;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
import info.mengnan.dialogerai.server.service.ChatSessionService;
import info.mengnan.dialogerai.server.service.MemberService;
import info.mengnan.dialogerai.server.service.ModelConfigService;
import info.mengnan.dialogerai.tool.ToolExecutionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static info.mengnan.dialogerai.common.param.MessageRole.*;
import static info.mengnan.dialogerai.rag.config.DefaultModelConfig.DEFAULT_SESSION;
import static info.mengnan.dialogerai.server.param.chat.ChatSessionResponse.DEFAULT_TITLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ChatMessageRepository chatMessageService;
    private final RagSourceStore ragSourceStore;
    private final ToolExecutionStore toolExecutionStore;
    private final ChatSessionService chatSessionService;
    private final MemberService memberService;
    private final ModelConfigService modelConfigService;
    private final DirectModelInvoker directModelInvoker;

    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        // 查询所有消息类型
        List<ChatMessage> dbMessages = chatMessageService.findChat(sessionId);

        // 找到最后一个 compress 消息的索引
        int lastCompressIndex = -1;
        for (int i = dbMessages.size() - 1; i >= 0; i--) {
            if (COMPRESS.equals(dbMessages.get(i).getRole())) {
                lastCompressIndex = i;
                break;
            }
        }

        // 过滤出要发送给 LLM 的消息（从 compress 之后开始收集）
        List<ChatMessage> filtered = new ArrayList<>();
        if (lastCompressIndex != -1) {
            // 从 compress 消息之后开始添加
            for (int i = lastCompressIndex + 1; i < dbMessages.size(); i++) {
                filtered.add(dbMessages.get(i));
            }
        } else {
            // 没有 compress 消息，添加所有
            filtered.addAll(dbMessages);
        }

        List<dev.langchain4j.data.message.ChatMessage> result = new ArrayList<>();
        for (ChatMessage chatMessage : filtered) {
            dev.langchain4j.data.message.ChatMessage userMessage = convertToChatMessage(chatMessage);
            result.add(userMessage);
        }

        return result;
    }

    @Override
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String sessionId = memoryId.toString();
        if (DEFAULT_SESSION.equals(sessionId)) {
            return;
        }

        dev.langchain4j.data.message.ChatMessage chatMessage = messages.get(messages.size() - 1);
        ChatMessage dbMessage = new ChatMessage();
        dbMessage.setSessionId(sessionId);

        if (chatMessage instanceof UserMessage msg) {
            dbMessage.setRole(USER.n());
            dbMessage.setContent(msg.singleText());
            dbMessage.setExtras(buildUserExtras(msg));
            chatMessageService.insert(dbMessage);

        } else if (chatMessage instanceof AiMessage msg) {
            dbMessage.setRole(ASSISTANT.n());
            String aiText = msg.text();
            dbMessage.setContent(aiText != null ? aiText : "");
            dbMessage.setExtras(buildAiExtras(msg));
            chatMessageService.insert(dbMessage);

            ragSourceStore.linkToMessage(sessionId, dbMessage.getId());
            if (!msg.hasToolExecutionRequests()) {
                toolExecutionStore.linkToMessage(sessionId, dbMessage.getId());
            }

        } else if (chatMessage instanceof SystemMessage msg) {
            dbMessage.setRole(SYSTEM.n());
            dbMessage.setContent(msg.text());
            chatMessageService.insert(dbMessage);

        } else if (chatMessage instanceof ToolExecutionResultMessage msg) {
            dbMessage.setRole(TOOL.n());
            dbMessage.setContent(msg.text());
            dbMessage.setExtras(buildToolExtras(msg));
            chatMessageService.insert(dbMessage);
        }

        ChatSession chatSession = chatSessionService.findBySessionId(sessionId);
        if (DEFAULT_TITLE.equals(chatSession.getTitle())) {
            titleGeneration(messages, chatSession);
        }
    }

    private void titleGeneration(List<dev.langchain4j.data.message.ChatMessage> messages, ChatSession chatSession) {
        List<String> textList = new ArrayList<>();
        for (dev.langchain4j.data.message.ChatMessage msg : messages) {
            if (msg instanceof UserMessage m) {
                textList.add(m.singleText());
            } else if (msg instanceof AiMessage m && m.text() != null) {
                textList.add(m.text());
            }
        }
        if (textList.size() < 2) return;

        MemberTeamContext ctx = memberService.resolveTeamContext(chatSession.getMemberId());
        if (ctx == null) {
            log.warn("Skip title generation:iteam context not found, memberId={}", chatSession.getMemberId());
            return;
        }

        ModelConfig modelConfig = modelConfigService.findModelById(ctx.defaultChatModelId());
        if (modelConfig == null) {
            log.warn("Skip title generation: default chat model not configured, teamId={}", ctx.teamId());
            return;
        }

        Map<String, Object> params = Map.of("query", textList);
        String title = directModelInvoker.directInvoke(
                ctx.ownerId(),
                "conversations.titleGeneration",
                "title_generation",
                params,
                modelConfig);
        chatSessionService.updateChatTitle(chatSession.getChatSessionId(), title);
    }

    private ChatMessageExtras buildUserExtras(UserMessage msg) {
        boolean hasName = msg.name() != null && !msg.name().isBlank();
        if (!hasName) return null;
        ChatMessageExtras ex = new ChatMessageExtras();
        ex.setUserName(msg.name());
        return ex;
    }

    private ChatMessageExtras buildAiExtras(AiMessage msg) {
        Map<String, Object> attrs = msg.attributes();
        boolean hasAttrs = attrs != null && !attrs.isEmpty();
        boolean hasThinking = msg.thinking() != null && !msg.thinking().isBlank();
        boolean hasTools = msg.hasToolExecutionRequests();
        if (!hasThinking && !hasTools && !hasAttrs) return null;

        ChatMessageExtras ex = new ChatMessageExtras();
        if (hasThinking) ex.setThinking(msg.thinking());

        if (hasTools) {
            List<ChatMessageExtras.ToolExecutionRequestSnapshot> snapshots = new ArrayList<>();
            for (ToolExecutionRequest r : msg.toolExecutionRequests()) {
                ChatMessageExtras.ToolExecutionRequestSnapshot s = new ChatMessageExtras.ToolExecutionRequestSnapshot();
                s.setId(r.id());
                s.setName(r.name());
                s.setArguments(r.arguments());
                snapshots.add(s);
            }
            ex.setToolExecutionRequests(snapshots);
        }
        if (hasAttrs) {
            ex.setAttributes(new HashMap<>(attrs));
        }
        return ex;
    }

    private ChatMessageExtras buildToolExtras(ToolExecutionResultMessage msg) {
        boolean hasId = msg.id() != null && !msg.id().isBlank();
        boolean hasName = msg.toolName() != null && !msg.toolName().isBlank();
        if (!hasId && !hasName) {
            return null;
        }
        ChatMessageExtras ex = new ChatMessageExtras();
        if (hasId) {
            ex.setToolCallId(msg.id());
        }
        if (hasName) {
            ex.setToolName(msg.toolName());
        }
        return ex;
    }


    private void saveCompressedSummary(String sessionId, String summary, List<ChatMessage> originalMessages) {
        ChatMessage lastMsg = originalMessages.get(originalMessages.size() - 1);

        ChatMessage summaryMessage = new ChatMessage();
        summaryMessage.setSessionId(sessionId);
        summaryMessage.setRole(COMPRESS.n());

        String summaryWithMeta = String.format("[历史对话摘要 - 压缩了 %d 条消息]\n%s", originalMessages.size(), summary);
        summaryMessage.setContent(summaryWithMeta);

        summaryMessage.setCreatedAt(lastMsg.getCreatedAt());
        chatMessageService.insert(summaryMessage);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        chatMessageService.deleteBySessionId(sessionId);
    }

    /**
     * 转换数据库消息转换成对应类型
     */
    private dev.langchain4j.data.message.ChatMessage convertToChatMessage(ChatMessage dbMessage) {
        dev.langchain4j.data.message.ChatMessage chatMessage;
        String content = dbMessage.getContent() != null ? dbMessage.getContent() : "";
        ChatMessageExtras ex = dbMessage.getExtras();

        if (USER.equals(dbMessage.getRole())) {
            if (ex != null && ex.getUserName() != null && !ex.getUserName().isBlank()) {
                chatMessage = UserMessage.from(ex.getUserName(), content);
            } else {
                chatMessage = UserMessage.from(content);
            }

        } else if (ASSISTANT.equals(dbMessage.getRole())) {
            if (ex == null || !hasAiExtras(ex)) {
                chatMessage = AiMessage.from(content);
            } else {
                AiMessage.Builder b = AiMessage.builder().text(content);
                if (ex.getThinking() != null && !ex.getThinking().isBlank()) {
                    b.thinking(ex.getThinking());
                }
                if (ex.getToolExecutionRequests() != null && !ex.getToolExecutionRequests().isEmpty()) {
                    List<ToolExecutionRequest> results = new ArrayList<>();
                    for (ChatMessageExtras.ToolExecutionRequestSnapshot s : ex.getToolExecutionRequests()) {
                        results.add(ToolExecutionRequest.builder()
                                .id(s.getId() != null ? s.getId() : "")
                                .name(s.getName() != null ? s.getName() : "")
                                .arguments(s.getArguments() != null ? s.getArguments() : "")
                                .build());
                    }
                    b.toolExecutionRequests(results);
                }
                if (ex.getAttributes() != null && !ex.getAttributes().isEmpty()) {
                    b.attributes(new HashMap<>(ex.getAttributes()));
                }
                chatMessage = b.build();
            }

        } else if (SYSTEM.equals(dbMessage.getRole())) {
            chatMessage = SystemMessage.from(content);

        } else if (COMPRESS.equals(dbMessage.getRole())) {
            chatMessage = UserMessage.from(content);

        } else if (TOOL.equals(dbMessage.getRole())) {
            String toolId = ex != null && ex.getToolCallId() != null ? ex.getToolCallId() : "";
            String toolName = ex != null && ex.getToolName() != null ? ex.getToolName() : "";
            chatMessage = ToolExecutionResultMessage.from(toolId, toolName, content);
        } else {
            throw new IllegalArgumentException("不支持的消息类型：" + dbMessage.getRole());
        }
        return chatMessage;
    }

    private static boolean hasAiExtras(ChatMessageExtras ex) {
        return (ex.getThinking() != null && !ex.getThinking().isBlank())
                || (ex.getToolExecutionRequests() != null && !ex.getToolExecutionRequests().isEmpty())
                || (ex.getAttributes() != null && !ex.getAttributes().isEmpty());
    }

}
