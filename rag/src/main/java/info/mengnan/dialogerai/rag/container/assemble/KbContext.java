package info.mengnan.dialogerai.rag.container.assemble;

import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver.KbIndexRef;

import java.util.List;

/**
 * 知识库上下文接口
 */
public interface KbContext {

    List<KbIndexRef> kbIndexRefs();
}
