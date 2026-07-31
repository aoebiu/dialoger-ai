package info.mengnan.dialogerai.common.param;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型附加能力枚举。用于标记 CHAT / STREAMING_CHAT 模型是否支持特殊能力。
 * 目前仅支持 vision（视觉理解）。后续可扩展 tool-call、thinking 等。
 */
public enum ModelCapability {

    VISION;

    public String n() {
        return this.name().toLowerCase();
    }

    /** 从存储的逗号分隔字符串解析出能力集合，忽略未知项。 */
    public static Set<ModelCapability> parse(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<ModelCapability> result = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String key = part.trim();
            if (key.isEmpty()) continue;
            for (ModelCapability c : values()) {
                if (c.n().equalsIgnoreCase(key) || c.name().equalsIgnoreCase(key)) {
                    result.add(c);
                    break;
                }
            }
        }
        return result;
    }

    /** 将能力集合序列化为逗号分隔字符串（按枚举定义顺序）。 */
    public static String join(Set<ModelCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) return null;
        return Arrays.stream(values())
                .filter(capabilities::contains)
                .map(ModelCapability::n)
                .collect(Collectors.joining(","));
    }

    public static boolean contains(String raw, ModelCapability target) {
        return parse(raw).contains(target);
    }
}
