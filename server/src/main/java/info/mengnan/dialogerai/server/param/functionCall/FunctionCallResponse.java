package info.mengnan.dialogerai.server.param.functionCall;

import info.mengnan.dialogerai.repository.entity.ChatToolDescription;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FunctionCallResponse {

    private Long id;
    private Long memberId;
    private String name;
    private String description;
    private String property;
    private String required;
    private String execute;
    private String generatePrompt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String creatorName;

    public static FunctionCallResponse from(ChatToolDescription tool, String creatorName) {
        FunctionCallResponse resp = new FunctionCallResponse();
        resp.setId(tool.getId());
        resp.setMemberId(tool.getMemberId());
        resp.setName(tool.getName());
        resp.setDescription(tool.getDescription());
        resp.setProperty(tool.getProperty());
        resp.setRequired(tool.getRequired());
        resp.setExecute(tool.getExecute());
        resp.setGeneratePrompt(tool.getGeneratePrompt());
        resp.setCreatedAt(tool.getCreatedAt());
        resp.setUpdatedAt(tool.getUpdatedAt());
        resp.setCreatorName(creatorName);
        return resp;
    }
}
