package info.mengnan.dialogerai.server.param.knowledgebase;

import lombok.Data;

@Data
public class KnowledgeBaseRequest {

    private String name;
    private String description;
    private String visibility;
    private Integer topK;
    private Double score;
    private Long memberId;
    private Long kbId;
}
