package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.service.DirectModelInvoker;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

/**
 * 图片处理服务
 * 提供图片 URL 到文本描述的转换功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private static final String IMAGE_PLACEHOLDER = "[图片]";

    private final DirectModelInvoker directModelInvoker;
    private final MemberService memberService;
    private final ModelConfigService modelConfigService;

    /**
     * 处理图片 URL，将图片转换为文本描述
     * 支持 base64 格式和 HTTP/HTTPS URL
     *
     * @param imageUrl 图片 URL
     * @param memberId 请求方成员 ID，用于定位所属团队的默认图像模型
     * @return 图片的文本描述，如果处理失败则返回 [图片]
     */
    public String processImageUrl(String imageUrl, Long memberId) {
        return processImageUrl(imageUrl, "identify_picture", resolveImageModelConfigFromMember(memberId));
    }

    /**
     * 处理图片 URL，将图片转换为文本描述
     * 支持 base64 格式和 HTTP URL
     *
     * @param imageUrl    图片 URL
     * @param modelConfig 视觉模型配置（调用方负责解析并保证非空）
     * @return 图片的文本描述，如果处理失败则返回 [图片]
     */
    public String processImageUrl(String imageUrl, ModelConfig modelConfig) {
        return processImageUrl(imageUrl, "identify_picture", modelConfig);
    }

    /**
     * 处理图片 URL，将图片转换为文本描述
     * 支持 base64 格式和 HTTP URL
     *
     * @param imageUrl       图片 URL
     * @param promptTemplate 提示词模板名称
     * @param modelConfig    视觉模型配置
     * @return 图片的文本描述，如果处理失败则返回 [图片]
     */
    public String processImageUrl(String imageUrl, String promptTemplate, ModelConfig modelConfig) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return IMAGE_PLACEHOLDER;
        }
        if (modelConfig == null) {
            return IMAGE_PLACEHOLDER;
        }

        // 处理 base64 格式的图片
        if (imageUrl.startsWith("data:image/")) {
            return processBase64Image(imageUrl, promptTemplate, modelConfig);
        }
        // 处理 HTTP URL
        else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return processHttpImage(imageUrl, promptTemplate, modelConfig);
        }

        log.warn("Unsupported image URL format: {}", imageUrl);
        return IMAGE_PLACEHOLDER;
    }

    private ModelConfig resolveImageModelConfigFromMember(Long memberId) {
        MemberTeamContext ctx = memberId == null ? null : memberService.resolveTeamContext(memberId);
        if (ctx == null) {
            log.warn("Image recognition skipped: team context missing, memberId={}", memberId);
            return null;
        }
        ModelConfig modelConfig = modelConfigService.findModelById(ctx.defaultImageModelId());
        if (modelConfig == null) {
            log.warn("Image recognition skipped: default image model not configured, teamId={}", ctx.teamId());
        }
        return modelConfig;
    }

    /**
     * 处理 base64 编码的图片
     *
     * @param dataUrl        base64 格式的图片 URL (data:image/xxx;base64,...)
     * @param promptTemplate 提示词模板名称
     * @param modelConfig    视觉模型配置
     * @return 图片的文本描述
     */
    private String processBase64Image(String dataUrl, String promptTemplate, ModelConfig modelConfig) {
        String[] parts = dataUrl.split(",", 2);
        if (parts.length != 2) {
            log.warn("Invalid base64 image format");
            return IMAGE_PLACEHOLDER;
        }

        // 提取 MIME 类型
        String mimeType = parts[0].split(";")[0].replace("data:", "");
        String base64Data = parts[1];

        // 解码 base64 数据
        byte[] imageData;
        try {
            imageData = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode base64 image data", e);
            return IMAGE_PLACEHOLDER;
        }

        // 调用 directModelInvoker 进行图片识别
        try {
            String imageDescription = directModelInvoker.imageToText(imageData, promptTemplate, mimeType, modelConfig);
            return imageDescription != null ? imageDescription : IMAGE_PLACEHOLDER;
        } catch (Exception e) {
            log.error("Failed to recognize image from base64", e);
            return IMAGE_PLACEHOLDER;
        }
    }

    /**
     * 处理 HTTP/HTTPS URL 的图片
     *
     * @param imageUrl       HTTP/HTTPS 格式的图片 URL
     * @param promptTemplate 提示词模板名称
     * @param modelConfig    视觉模型配置
     * @return 图片的文本描述
     */
    private String processHttpImage(String imageUrl, String promptTemplate, ModelConfig modelConfig) {
        byte[] imageData;
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            imageData = inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to download image from URL: {}", imageUrl, e);
            return IMAGE_PLACEHOLDER;
        }

        String mimeType = inferMimeTypeFromUrl(imageUrl);
        try {
            // 调用 directModelInvoker 进行图片识别
            String imageDescription = directModelInvoker.imageToText(imageData, promptTemplate, mimeType, modelConfig);
            return imageDescription != null ? imageDescription : IMAGE_PLACEHOLDER;
        } catch (Exception e) {
            log.error("Failed to recognize image from HTTP URL: {}", imageUrl, e);
            return IMAGE_PLACEHOLDER;
        }
    }

    /**
     * 从 URL 推断 MIME 类型
     *
     * @param url 图片 URL
     * @return MIME 类型，默认返回 image/png
     */
    private String inferMimeTypeFromUrl(String url) {
        String mimeType = URLConnection.guessContentTypeFromName(url);
        return mimeType != null ? mimeType : "image/png";
    }
}
