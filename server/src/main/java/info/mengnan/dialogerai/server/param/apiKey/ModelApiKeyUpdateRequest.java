package info.mengnan.dialogerai.server.param.apiKey;

import lombok.Data;

import java.util.List;

/**
 * 更新模型 API Key 的可编辑字段（描述、能力）
 */
@Data
public class ModelApiKeyUpdateRequest {

    private String description;

    private List<String> capabilities;
}
