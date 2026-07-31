package info.mengnan.dialogerai.server.config;

import info.mengnan.dialogerai.kb.core.DynamicEmbeddingStoreRegistry;
import info.mengnan.dialogerai.server.core.DocumentEmbedding;
import info.mengnan.dialogerai.kb.core.DocumentImageExtractor;
import info.mengnan.dialogerai.kb.core.SequentialDocumentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KbConfiguration {

    @Bean
    public DocumentImageExtractor documentImageExtractor() {
        return new DocumentImageExtractor();
    }

    @Bean
    public SequentialDocumentExtractor sequentialDocumentExtractor() {
        return new SequentialDocumentExtractor();
    }

    @Bean
    public DocumentEmbedding documentEmbedding(DynamicEmbeddingStoreRegistry embeddingStoreRegistry) {
        return new DocumentEmbedding(embeddingStoreRegistry);
    }

}
