package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatMessage;
import info.mengnan.dialogerai.repository.entity.ChatSession;
import info.mengnan.dialogerai.repository.repo.ChatMessageRepository;
import info.mengnan.dialogerai.repository.repo.ChatSessionRepository;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static info.mengnan.dialogerai.common.param.MessageRole.ASSISTANT;
import static info.mengnan.dialogerai.common.param.MessageRole.USER;
import static info.mengnan.dialogerai.server.param.chat.ChatSessionResponse.DEFAULT_TITLE;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DirectModelInvoker directModelInvoker;

    public ChatSession findBySessionId(String sessionId) {
        return chatSessionRepository.findBySessionId(sessionId);
    }

    public List<ChatSession> findAll(Long memberId) {
        return chatSessionRepository.findAllByMemberId(memberId);
    }

    /**
     * 为 memberId 创建新会话。若最近一个会话为空（无消息），则不创建，返回 null。
     */
    public ChatSession createChat(Long memberId) {
        ChatSession last = chatSessionRepository.findLastByMemberId(memberId);
        if (last != null && chatMessageRepository.findChat(last.getChatSessionId()).isEmpty()) {
            return null;
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        ChatSession session = new ChatSession();
        session.setChatSessionId(sessionId);
        session.setMemberId(memberId);
        session.setTitle(DEFAULT_TITLE);
        chatSessionRepository.createChat(session);
        return session;
    }

    /**
     * 若会话标题为默认值且已有足够消息，则生成并持久化标题，返回新标题；否则返回 null。
     */
    public String generateTitle(String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null || !DEFAULT_TITLE.equals(session.getTitle())) {
            return null;
        }
        List<String> contents = chatMessageRepository.findChat(sessionId, List.of(ASSISTANT.n(), USER.n()))
                .stream()
                .map(ChatMessage::getContent)
                .limit(3)
                .toList();
        if (contents.size() < 2) {
            return null;
        }
        Map<String, Object> params = Map.of("query", contents);
        String title = directModelInvoker.directInvoke("conversations.titleGeneration", "title_generation", params);
        chatSessionRepository.updateChatTitle(sessionId, title);
        return title;
    }

    public void deleteBySessionId(String sessionId) {
        chatSessionRepository.deleteBySessionId(sessionId);
    }
}
