package info.mengnan.dialogerai.rag.config.params;

import info.mengnan.dialogerai.common.json.JSONObject;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.common.validation.ModelParamValidator;
import info.mengnan.dialogerai.rag.provider.ModelParamSchemaRegistry;
import info.mengnan.dialogerai.rag.provider.params.qwen.QwenChatParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelParamValidatorTest {

    @Test
    void mergeDefaultsWhenParamsEmpty() {
        ModelConfig config = new ModelConfig();
        QwenChatParams params = config.getParams(QwenChatParams.class);
        assertEquals(0.8, params.getTopP());
        assertEquals(50, params.getTopK());
        assertEquals(0.7f, params.getTemperature());
    }

    @Test
    void rejectUnknownFields() {
        JSONObject payload = new JSONObject();
        payload.set("topP", 0.5);
        payload.set("unknownField", "x");
        assertThrows(IllegalArgumentException.class,
                () -> ModelParamValidator.validate("Qwen", ModelType.CHAT, payload, ModelParamSchemaRegistry.LOOKUP));
    }

    @Test
    void validateRange() {
        JSONObject payload = new JSONObject();
        payload.set("topP", 2.0);
        assertThrows(IllegalArgumentException.class,
                () -> ModelParamValidator.validate("Qwen", ModelType.CHAT, payload, ModelParamSchemaRegistry.LOOKUP));
    }
}
