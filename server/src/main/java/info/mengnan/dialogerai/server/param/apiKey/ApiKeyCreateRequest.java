package info.mengnan.dialogerai.server.param.apiKey;

import lombok.Data;

/**
 * 创建 API Key 请求
 */
@Data
public class ApiKeyCreateRequest {

    /** API Key 名称/描述 */
    private String name;

    /** 有效天数，为空表示永不过期 */
    private Integer expiresInDays;

    /** 绑定的 Agent 配置 ID，为空表示暂不绑定 */
    private Long chatAgentOptionId;
}
