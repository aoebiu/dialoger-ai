package info.mengnan.dialogerai.rag.config.params;

import info.mengnan.dialogerai.rag.provider.ModelParamSchemaRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelParamBindingConsistencyTest {

    @Test
    void bootstrapRegistersAllProviders() {
        assertFalse(ModelParamSchemaRegistry.allBindings().isEmpty());
        assertNotNull(ModelParamSchemaRegistry.allBindings().get("Qwen"));
        assertNotNull(ModelParamSchemaRegistry.allBindings().get("OpenAI"));
        assertNotNull(ModelParamSchemaRegistry.allBindings().get("Ollama"));
    }

    @Test
    void listAllSchemasReturnsNestedStructure() {
        var all = ModelParamSchemaRegistry.listAllSchemas();
        assertFalse(all.isEmpty());
        assertTrue(all.containsKey("Qwen"));
        assertNotNull(all.get("Qwen").get("chat"));
        assertNotNull(all.get("Qwen").get("chat").getFields());
    }
}
