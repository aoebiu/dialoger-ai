package info.mengnan.dialogerai.server.param.apiKey;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目 API Key 视图对象
 */
@Data
public class ProjectApiKeyResponse {

    private Long id;

    /**
     * API Key 名称/描述
     */
    private String name;

    /**
     * 绑定的 Agent 配置 ID
     */
    private Long chatAgentOptionId;

    /**
     * 绑定的 Agent 配置名称（便于列表直接展示）
     */
    private String chatAgentOptionName;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 脱敏后的 API Key
     */
    private String apiKey;
}