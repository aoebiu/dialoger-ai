package info.mengnan.dialogerai.rag.container.assemble;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.List;
import java.util.Map;

/**
 * AI 服务组件组装器抽象类
 * @param <K> 知识库上下文类型，必须实现 {@link KbContext} 以提供索引引用
 */
public abstract class AiServiceAssembler<K extends KbContext> {

    /**
     * 构建模型配置。
     */
    protected abstract AssembledModels assembledModels();

    /**
     * 构建工具映射。
     */
    protected abstract Map<ToolSpecification, ToolExecutor> toolMap();

    /**
     * 构建知识库上下文。
     * <p>
     * 返回值必须实现 {@link KbContext}，其中包含知识库索引引用列表，
     * 供 ChatService 直接通过 {@code kbContext().kbIndexRefs()} 获取。
     */
    protected abstract List<K> kbContext();


    public record AiComponents<K extends KbContext>(
            AssembledModels assembledModels,
            Map<ToolSpecification, ToolExecutor> toolMap,
            List<K> kbContext
    ) {}

    public final AiComponents<K> assemble() {
        return new AiComponents<>(assembledModels(), toolMap(), kbContext());
    }
}
