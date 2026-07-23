package info.mengnan.dialogerai.server.param.apiKey;

import info.mengnan.dialogerai.repository.entity.ChatApiKey;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ModelApiKeyResponse {

    private Long id;
    private String modelName;
    private String modelProvider;
    private String keyType;
    private String description;
    private Boolean enabled;
    private String maskedApiKey;
    private LocalDateTime createdAt;

    public static ModelApiKeyResponse from(ChatApiKey key) {
        ModelApiKeyResponse response = new ModelApiKeyResponse();
        response.setId(key.getId());
        response.setModelName(key.getModelName());
        response.setModelProvider(key.getModelProvider());
        response.setKeyType(key.getKeyType());
        response.setDescription(key.getDescription());
        response.setEnabled(key.getEnabled() == null ? true : key.getEnabled());
        response.setCreatedAt(key.getCreatedAt());
        return response;
    }

    public static ModelApiKeyResponse fromMasked(ChatApiKey key) {
        ModelApiKeyResponse response = from(key);
        response.setMaskedApiKey(maskApiKey(key.getApiKey()));
        return response;
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}