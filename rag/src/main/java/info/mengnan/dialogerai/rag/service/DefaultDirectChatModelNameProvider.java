package info.mengnan.dialogerai.rag.service;

/**
 * 根据 ownerId 解析团队默认 direct chat 模型名称
 */
@FunctionalInterface
public interface DefaultDirectChatModelNameProvider {

    String findDefaultDirectChatModelName(Long ownerId);
}
