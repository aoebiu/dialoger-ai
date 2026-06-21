package info.mengnan.dialogerai.server.param.chat;

import lombok.Data;

import java.util.List;

@Data
public class ChatConversations {
    private String sessionId;
    private Long memberId;
    private String title;
    private List<Long> sourceIds;
    private List<Long> toolExecutionIds;


    public ChatConversations(Long memberId, String sessionId) {
        this.sessionId = sessionId;
        this.memberId = memberId;
    }
}
