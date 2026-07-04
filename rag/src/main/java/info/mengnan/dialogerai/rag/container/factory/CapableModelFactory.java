package info.mengnan.dialogerai.rag.container.factory;

import info.mengnan.dialogerai.common.classpath.ClasspathScanner;
import info.mengnan.dialogerai.common.param.ModelType;
import info.mengnan.dialogerai.rag.config.ModelConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 通用模型工厂实现
 * 从 META-INF/model-mapping/*.properties 配置文件中加载 Provider 映射，
 * 运行时通过反射创建模型实例。
 *
 * 新增 Provider 只需在 classpath 中添加对应的 properties 文件，无需编写 Java 代码。
 */
@Slf4j
public class CapableModelFactory implements UniversalModelFactory {

    private static final String MAPPING_LOCATION = "META-INF/model-mapping/";

    private final Map<String, Map<ModelType, String>> modelClassMapping = new HashMap<>();
    private final Map<String, ModelFactory> modelFactories = new HashMap<>();

    public CapableModelFactory() {
        loadMappings();
    }

    private void loadMappings() {
        ClasspathScanner.forEachResource(MAPPING_LOCATION, ".properties", (entryName, inputStream) -> {
            try {
                loadSingleMapping(inputStream, entryName);
            } catch (IOException ex) {
                log.warn("Failed to load mapping file: {}", entryName, ex);
            }
        });
    }

    private void loadSingleMapping(InputStream is, String source) throws IOException {
        Properties props = new Properties();
        props.load(is);

        String providerCode = props.getProperty("providerCode");
        if (providerCode == null || providerCode.isBlank()) {
            log.warn("Skipping mapping file without providerCode: {}", source);
            return;
        }

        Map<ModelType, String> typeMapping = new HashMap<>();
        for (ModelType modelType : ModelType.values()) {
            String className = props.getProperty(modelType.name());
            if (className != null && !className.isBlank()) {
                typeMapping.put(modelType, className.trim());
            }
        }
        modelClassMapping.put(providerCode, typeMapping);

        String applierClassName = props.getProperty("paramApplier");
        if (applierClassName != null && !applierClassName.isBlank()) {
            try {
                Class<?> applierClass = Class.forName(applierClassName.trim());
                ModelFactory factory = (ModelFactory) applierClass.getDeclaredConstructor().newInstance();
                modelFactories.put(providerCode, factory);
            } catch (Exception e) {
                log.warn("Failed to load ParamApplier '{}' for provider '{}': {}",
                        applierClassName, providerCode, e.getMessage());
            }
        }

        log.info("Loaded model mapping for provider: {} from {} (types: {})",
                providerCode, source, typeMapping.keySet());
    }

    @Override
    public Object createModel(ModelConfig modelConfig, ModelType modelType) {
        String providerCode = modelConfig.getModelProvider();

        // 优先委托给各 Provider 的 ModelFactory，由其负责完整的模型构建与参数注入
        ModelFactory factory = modelFactories.get(providerCode);
        if (factory != null) {
            log.info("Creating {} model for provider '{}' via ModelFactory", modelType, providerCode);
            return factory.createModel(modelConfig, modelType);
        }

        // 暂时不支持的模型通过反射方式
        return createModelByReflection(modelConfig, modelType, providerCode);
    }

    private Object createModelByReflection(ModelConfig modelConfig, ModelType modelType, String providerCode) {
        try {
            Map<ModelType, String> providerModels = modelClassMapping.get(providerCode);
            if (providerModels == null) {
                throw new UnsupportedOperationException(
                        "Unsupported model provider: " + providerCode);
            }

            String className = providerModels.get(modelType);
            if (className == null) {
                throw new UnsupportedOperationException(
                        "Model type '" + modelType + "' is not supported for provider: " + providerCode);
            }

            Class<?> modelClass = Class.forName(className);
            Method builderMethod = modelClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();

            try {
                Method apiKeyMethod = builderClass.getMethod("apiKey", String.class);
                builder = apiKeyMethod.invoke(builder, modelConfig.getApiKey());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have apiKey method, skipping", className);
            }

            try {
                Method modelNameMethod = builderClass.getMethod("modelName", String.class);
                builder = modelNameMethod.invoke(builder, modelConfig.getModelName());
            } catch (NoSuchMethodException e) {
                log.debug("Model {} does not have modelName method, skipping", className);
            }

            Object model = builderClass.getMethod("build").invoke(builder);

            log.info("Successfully created {} model: {} (provider: {}, modelName: {})",
                    modelType, className, providerCode, modelConfig.getModelName());

            return model;
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Model class not found in classpath. Please add the corresponding dependency for provider: "
                            + modelConfig.getModelProvider(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create model for provider: " + modelConfig.getModelProvider()
                            + ", type: " + modelType, e);
        }
    }
}
