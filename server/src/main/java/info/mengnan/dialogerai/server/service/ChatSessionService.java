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
    public void deleteBySessionId(String sessionId) {
        chatSessionRepository.deleteBySessionId(sessionId);
    }

    public void updateChatTitle(String sessionId, String title) {
        chatSessionRepository.updateChatTitle(sessionId, title);
    }
}
