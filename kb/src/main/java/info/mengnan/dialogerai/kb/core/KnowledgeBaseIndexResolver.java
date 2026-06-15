package info.mengnan.dialogerai.kb.core;

import java.util.List;

/**
 * 解析用户下可用于 RAG 检索的知识库索引列表。
 */
public interface KnowledgeBaseIndexResolver {

    List<KbIndexRef> resolveActiveIndexes(Long memberId);

    record KbIndexRef(String indexName, String displayName,Integer topK, Double score) {}
}
