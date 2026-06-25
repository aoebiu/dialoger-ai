package info.mengnan.dialogerai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.kb.core.KnowledgeBaseIndexResolver.KbIndexRef;
import info.mengnan.dialogerai.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import info.mengnan.dialogerai.rag.container.assemble.AiServiceAssembler;
import info.mengnan.dialogerai.rag.container.assemble.AssembledModels;
import info.mengnan.dialogerai.rag.container.factory.UniversalModelFactory;
import info.mengnan.dialogerai.rag.handler.StreamingResponseHandler;
import info.mengnan.dialogerai.rag.injector.CapturingContentInjector;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import info.mengnan.dialogerai.rag.container.assemble.KbContext;
import info.mengnan.dialogerai.rag.injector.RagSourceStore;
import info.mengnan.dialogerai.tool.ToolExecutionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static dev.langchain4j.rag.query.router.LanguageModelQueryRouter.FallbackStrategy.DO_NOT_ROUTE;
import static info.mengnan.dialogerai.common.param.ModelType.*;
import static info.mengnan.dialogerai.rag.constant.promptTemplate.PromptTemplateConstant.*;


@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatMemoryStore chatMemoryStore;
    private final UniversalModelFactory modelFactory;
    private final DynamicEmbeddingStoreRegistry embeddingStoreRegistry;
    private final RagSourceStore ragSourceStore;
    private final ToolExecutionStore toolExecutionStore;
    private final Executor ragExecutor;
    private final Executor toolExecutor;

    /**
     * 流式RAG对话 - 使用回调处理器
     *
     * @param sessionId 会话id
     * @param message   消息
     * @param handler   流式响应处理器
     */
    public void chatStreaming(String sessionId,
                              String message,
                              StreamingResponseHandler handler,
                              AiServiceAssembler<? extends KbContext> aiServiceAssembler) {
        AiServiceAssembler.AiComponents<? extends KbContext> aiComponents = aiServiceAssembler.assemble();
        AssembledModels assembledModels = aiComponents.assembledModels();
        Map<ModelType, ModelConfig> configs = assembledModels.getConfigs();

        AssistantUnique assistantUnique = new AssistantUniqueBuilder(modelFactory)
                .configureRag(assembledModels, aiComponents.kbContext(), embeddingStoreRegistry, ragSourceStore, ragExecutor, sessionId)
                .configureStreamingChatModel(configs.get(STREAMING_CHAT))
                .configureChatModel(configs.get(CHAT))
                .configureModerationModel(configs.get(MODERATION))
                .configureTools(assembledModels.getTools(), aiComponents.toolMap(), sessionId)
                .chatMemoryProvider(assembledModels)
                .build();
        try {
            TokenStream tokenStream = assistantUnique.chatStreaming(sessionId, message);
            tokenStream.onPartialResponse(token -> {
                        if (!handler.isCancelled()) {
                            handler.onToken(token);
                        }
                    })
                    .onCompleteResponse(response -> {
                        if (!handler.isCancelled()) {
                            handler.onComplete(response.aiMessage().text());
                        }
                    })
                    .onError(error -> {
                        if (!handler.isCancelled()) {
                            handler.onError(error);
                        }
                    })
                    .start();
        } catch (Exception e) {
            handler.onError(e);
        }
    }

    public class AssistantUniqueBuilder {
        private final AiServices<AssistantUnique> aiServices;
        private final UniversalModelFactory modelFactory;

        private AssistantUniqueBuilder(UniversalModelFactory modelFactory) {
            this.modelFactory = modelFactory;
            this.aiServices = AiServices.builder(AssistantUnique.class);
        }

        private AssistantUniqueBuilder configureRag(AssembledModels assembledModels,
                                                    List<? extends KbContext> kbContexts,
                                                    DynamicEmbeddingStoreRegistry embeddingStoreRegistry,
                                                    RagSourceStore ragSourceStore,
                                                    Executor ragExecutor,
                                                    String sessionId) {
            if (!assembledModels.getRag()) {
                return this;
            }
            aiServices.storeRetrievedContentInChatMemory(false);
            DefaultRetrievalAugmentor.DefaultRetrievalAugmentorBuilder ragBuilder = DefaultRetrievalAugmentor.builder();
            ragBuilder.executor(ragExecutor);

            if (assembledModels.getContentAggregator()) {
                ragBuilder.contentAggregator(createContentAggregator(assembledModels.getConfigs().get(SCORING)));
            }
            if (assembledModels.getTransform() != null) {
                ragBuilder.queryTransformer(new DefaultQueryTransformer());
            }

            ragBuilder.queryRouter(createQueryRouter(assembledModels.getConfigs(), kbContexts, embeddingStoreRegistry));
            ragBuilder.contentInjector(new CapturingContentInjector(sessionId, ragSourceStore));
            aiServices.retrievalAugmentor(ragBuilder.build());
            return this;
        }

        private AssistantUniqueBuilder configureTools(boolean hasTools,
                                                      Map<ToolSpecification, ToolExecutor> toolMap,
                                                      String sessionId) {
            if (hasTools) {
                aiServices.tools(toolMap);
                aiServices.executeToolsConcurrently(toolExecutor);
                aiServices.beforeToolExecution(before -> {
                    ToolExecutionRequest request = before.request();
                    toolExecutionStore.savePending(
                            sessionId,
                            new ToolExecutionStore.ToolExecution(request.id(), request.name(), request.arguments(), null)
                    );
                });
                aiServices.afterToolExecution(execution -> {
                    ToolExecutionRequest request = execution.request();
                    String result = execution.result() != null ? execution.result() : "";
                    if (!toolExecutionStore.updateResult(sessionId, request.id(), result)) {
                        toolExecutionStore.savePending(
                                sessionId,
                                new ToolExecutionStore.ToolExecution(request.id(), request.name(), request.arguments(), result)
                        );
                    }
                });
            }
            return this;
        }


        private AssistantUniqueBuilder configureStreamingChatModel(ModelConfig streamingChatConfig) {
            if (streamingChatConfig == null) {
                return this;
            }
            aiServices.streamingChatModel(modelFactory.createStreamingChatModel(streamingChatConfig));
            return this;
        }

        private AssistantUniqueBuilder configureChatModel(ModelConfig chatConfig) {
            if (chatConfig == null) {
                return this;
            }
            aiServices.chatModel(modelFactory.createChatModel(chatConfig));
            return this;
        }

        private AssistantUniqueBuilder configureModerationModel(ModelConfig moderationConfig) {
            ModerationModel model = moderationConfig == null
                    ? new DisabledModerationModel()
                    : modelFactory.createModerationModel(moderationConfig);
            aiServices.moderationModel(model);
            return this;
        }

        private ContentAggregator createContentAggregator(ModelConfig scoringConfig) {
            if (scoringConfig == null) {
                return new DefaultContentAggregator();
            }
            ScoringModel scoringModel = modelFactory.createScoringModel(scoringConfig);
            return ReRankingContentAggregator.builder()
                    .scoringModel(scoringModel)
                    .querySelector(queryToContents -> queryToContents.entrySet().iterator().next().getKey())
                    .build();
        }

        private MessageWindowChatMemory createChatMemory(Object memoryId, AssembledModels assembledModels) {
            return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(assembledModels.getMaxMessages())
                    .chatMemoryStore(assembledModels.getInDB() ? chatMemoryStore : null)
                    .build();
        }

        private QueryRouter createQueryRouter(Map<ModelType, ModelConfig> configs,
                                              List<? extends KbContext> kbContexts,
                                              DynamicEmbeddingStoreRegistry embeddingStoreRegistry) {
            EmbeddingModel embeddingModel = modelFactory.createEmbeddingModel(configs.get(EMBEDDING));
            if (embeddingModel == null) {
                log.warn("Failed to create embedding model");
                return new DefaultQueryRouter();
            }

            Map<ContentRetriever, String> retrieverToKbName = kbContexts.stream()
                    .flatMap(kbContext -> kbContext.kbIndexRefs().stream())
                    .collect(
                            HashMap::new,
                            (map, ref) -> map.put(
                                    createContentRetriever(ref, embeddingModel, embeddingStoreRegistry),
                                    ref.displayName()
                            ),
                            HashMap::putAll
                    );
            if (retrieverToKbName.isEmpty()) {
                return new DefaultQueryRouter();
            }
            ChatModel chatModel = modelFactory.createChatModel(configs.get(CHAT));
            if (chatModel == null) {
                return new DefaultQueryRouter(retrieverToKbName.keySet());
            }
            return new LanguageModelQueryRouter(chatModel, retrieverToKbName, QUERY_ROUTER_PROMPT_TEMPLATE, DO_NOT_ROUTE);
        }

        private ContentRetriever createContentRetriever(KbIndexRef ref,
                                                                 EmbeddingModel embeddingModel,
                                                                 DynamicEmbeddingStoreRegistry embeddingStoreRegistry) {
            try {
                EmbeddingStore<TextSegment> embeddingStore = embeddingStoreRegistry.createEmbeddingStore(ref.indexName());
                ContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder()
                        .maxResults(ref.topK())
                        .minScore(ref.score())
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .build();
                return query -> {
                    List<Content> contents = baseRetriever.retrieve(query);
                    contents.forEach(c -> {
                        c.textSegment().metadata().put("indexName", ref.indexName());
                        c.textSegment().metadata().put("kbName", ref.displayName());
                    });
                    return contents;
                };
            } catch (Exception e) {
                log.error("Failed to create ContentRetriever for kb: {}", ref.displayName(), e);
                return null;
            }
        }

        public AssistantUniqueBuilder chatMemoryProvider(AssembledModels assembledModels) {
            aiServices.chatMemoryProvider(memoryId -> createChatMemory(memoryId, assembledModels));
            return this;
        }

        public AssistantUnique build() {
            return aiServices.build();
        }
    }
}
