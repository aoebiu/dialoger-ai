package info.mengnan.dialogerai.server.param.apiKey;

import lombok.Data;

/**
 * 更新 API Key 绑定信息（例如切换 Agent）
 */
@Data
public class ApiKeyUpdateRequest {

    /** 绑定的 Agent 配置 ID；显式传 null 表示解绑 */
    private Long chatAgentOptionId;
}
