package info.mengnan.dialogerai.rag.provider.params.qwen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.common.validation.ModelParam;
import info.mengnan.dialogerai.common.validation.ModelParams;
import lombok.Data;

@Data
@ModelParams(provider = "Qwen", types = ModelType.IMAGE)
public class QwenImageParams {

    @ModelParam(description = "参考图强度", min = 0, max = 1, defaultValue = "0.5")
    private Float refStrength;

    @ModelParam(description = "随机种子")
    private Integer seed;

    @ModelParam(description = "反向提示词")
    private String negativePrompt;

    @ModelParam(description = "是否扩展提示词", defaultValue = "true")
    private Boolean promptExtend;

    @ModelParam(description = "是否添加水印", defaultValue = "false")
    private Boolean watermark;

    @ModelParam(description = "图像风格", defaultValue = "<auto>")
    private Style style;

    @ModelParam(description = "图像尺寸", defaultValue = "1024*1024")
    private Size size;

    @ModelParam(description = "参考图模式")
    private RefMode refMode;

    public enum Style {
        AUTO("<auto>"),
        CARTOON_3D("<3d cartoon>"),
        ANIME("<anime>"),
        OIL_PAINTING("<oil painting>"),
        WATERCOLOR("<watercolor>"),
        SKETCH("<sketch>"),
        CHINESE_PAINTING("<chinese painting>"),
        FLAT_ILLUSTRATION("<flat illustration>");

        private final String apiValue;

        Style(String apiValue) {
            this.apiValue = apiValue;
        }

        @JsonValue
        public String getApiValue() {
            return apiValue;
        }

        @JsonCreator
        public static Style fromValue(String value) {
            for (Style item : values()) {
                if (item.apiValue.equals(value)) {
                    return item;
                }
            }
            throw new IllegalArgumentException("Unknown style: " + value);
        }
    }

    public enum Size {
        SIZE_1024("1024*1024"),
        SIZE_720_1280("720*1280"),
        SIZE_1280_720("1280*720"),
        SIZE_768_1152("768*1152"),
        SIZE_1152_768("1152*768");

        private final String apiValue;

        Size(String apiValue) {
            this.apiValue = apiValue;
        }

        @JsonValue
        public String getApiValue() {
            return apiValue;
        }

        @JsonCreator
        public static Size fromValue(String value) {
            for (Size item : values()) {
                if (item.apiValue.equals(value)) {
                    return item;
                }
            }
            throw new IllegalArgumentException("Unknown size: " + value);
        }
    }

    public enum RefMode {
        REPAINT, REFONLY;

        @JsonValue
        public String getApiValue() {
            return name();
        }

        @JsonCreator
        public static RefMode fromValue(String value) {
            return RefMode.valueOf(value);
        }
    }
}
