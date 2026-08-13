package info.mengnan.dialogerai.common.param;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelType {

    CHAT,
    STREAMING_CHAT,
    EMBEDDING,
    SCORING,
    MODERATION,
    // 生成图片的模型不是识别图片的模型
    IMAGE
    ;


    public String n() {
        return this.name().toLowerCase();
    }
}
