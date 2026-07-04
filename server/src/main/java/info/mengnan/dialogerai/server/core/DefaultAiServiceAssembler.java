package info.mengnan.dialogerai.server.core;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver.KbIndexRef;
import info.mengnan.dialogerai.rag.container.assemble.AiServiceAssembler;
import info.mengnan.dialogerai.rag.container.assemble.AssembledModels;
import info.mengnan.dialogerai.rag.container.assemble.KbContext;
import info.mengnan.dialogerai.server.service.AgentOptionService;
import info.mengnan.dialogerai.server.service.RagAdapterService;
import info.mengnan.dialogerai.server.service.ToolAdapterService;
import info.mengnan.dialogerai.server.util.SpringContextHolder;

import java.util.List;
import java.util.Map;

public class DefaultAiServiceAssembler extends AiServiceAssembler<DefaultAiServiceAssembler.DefaultKbContext> {

    private final Long memberId;
    private final Long optionId;
    private final RagAdapterService ragAdapterService;
    private final ToolAdapterService toolAdapterService;
    private final AgentOptionService agentOptionService;

    public DefaultAiServiceAssembler(Long memberId, Long optionId) {
        this.memberId = memberId;
        this.optionId = optionId;
        this.ragAdapterService = SpringContextHolder.getBean(RagAdapterService.class);
        this.toolAdapterService = SpringContextHolder.getBean(ToolAdapterService.class);
        this.agentOptionService = SpringContextHolder.getBean(AgentOptionService.class);
    }

    @Override
    protected AssembledModels assembledModels() {
        return ragAdapterService.assembleModels(optionId);
    }

    @Override
    protected Map<ToolSpecification, ToolExecutor> toolMap() {
        return toolAdapterService.dynamicTools(memberId);
    }

    @Override
    protected List<DefaultKbContext> kbContext() {
        List<KbIndexRef> refs = agentOptionService.resolveBoundKbIndexRefs(optionId);
        return refs.stream()
                .map(ref -> new DefaultKbContext(List.of(ref)))
                .toList();
    }

    /**
     * 知识库上下文：索引引用列表与对应的知识库实体保持同序。
     *
     */
    public class DefaultKbContext implements KbContext {
        private final List<KbIndexRef> kbIndexRefs;

        /**
         * @param kbIndexRefs RAG 检索所需的索引引用列表
         */
        public DefaultKbContext(List<KbIndexRef> kbIndexRefs) {
            this.kbIndexRefs = kbIndexRefs;
        }

        @Override
        public List<KbIndexRef> kbIndexRefs() {
            return kbIndexRefs;
        }
    }
}
