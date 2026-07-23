package info.mengnan.dialogerai.rag;

import dev.langchain4j.service.*;
import dev.langchain4j.service.memory.ChatMemoryAccess;

public interface AssistantUnique extends ChatMemoryAccess{

    @Moderate
    TokenStream chatStreaming(@MemoryId String memoryId, @UserMessage String userMessage);

}
