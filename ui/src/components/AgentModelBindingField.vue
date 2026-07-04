<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import ModelParamFields from '@/components/ModelParamFields.vue'
import { ensureModelParamSchemas, lookupModelParamSchema } from '@/api/model'
import type { ModelParamFieldSchema } from '@/api/model'
import type { AgentModelTypeKey, BindableModelOption } from '@/api/agentOption'
import type { ModelBindingForm } from '@/utils/modelParams'
import { countConfiguredParams } from '@/utils/modelParams'
import {
  AGENT_MODEL_UI_CONTEXT_KEY,
  type AgentModelUiContext,
} from '@/constants/agentModelUi'

const props = withDefaults(
  defineProps<{
    modelTypeKey: AgentModelTypeKey
    label: string
    binding: ModelBindingForm
    models: BindableModelOption[]
    editing?: boolean
    viewMode?: boolean
    optional?: boolean
    disabled?: boolean
    emptyOptionLabel?: string
    fieldClass?: string
    /** 可覆盖 inject 的场景样式，默认 settings */
    uiContext?: AgentModelUiContext
  }>(),
  {
    editing: true,
    viewMode: false,
    optional: false,
    disabled: false,
    emptyOptionLabel: '不绑定',
    fieldClass: '',
    uiContext: undefined,
  },
)

const injectedUiContext = inject(AGENT_MODEL_UI_CONTEXT_KEY, undefined)
const uiContext = computed<AgentModelUiContext>(
  () => props.uiContext ?? injectedUiContext?.value ?? 'settings',
)

const schemaFields = ref<ModelParamFieldSchema[]>([])
const schemaLoading = ref(false)
const schemaLoaded = ref(false)
const schemaError = ref('')
const paramsExpanded = ref(false)

const isWizardField = computed(() => props.fieldClass.includes('agent-field'))

const selectedModel = computed(() =>
  props.models.find((m) => m.modelName === props.binding.modelName) ?? null,
)

const displayText = computed(() => {
  if (!props.binding.modelName) return '—'
  const model = selectedModel.value
  return model ? `${model.modelName}（${model.modelProvider}）` : props.binding.modelName
})

const configuredCount = computed(() => countConfiguredParams(props.binding.params))
const showParamToggle = computed(
  () => Boolean(props.binding.modelName) && schemaFields.value.length > 0,
)

function resolveProvider(): string | null {
  return selectedModel.value?.modelProvider ?? null
}

async function loadSchema(resetParams: boolean) {
  schemaLoaded.value = false
  schemaError.value = ''
  schemaFields.value = []

  const modelName = props.binding.modelName?.trim()
  if (!modelName) return

  const provider = resolveProvider()
  if (!provider) return

  schemaLoading.value = true
  try {
    const allSchemas = await ensureModelParamSchemas()
    const schema = lookupModelParamSchema(allSchemas, provider, props.modelTypeKey)
    schemaFields.value = schema?.fields ?? []
    if (resetParams) {
      props.binding.params = {}
      paramsExpanded.value = false
    }
  } catch {
    schemaError.value = '加载参数配置失败'
  } finally {
    schemaLoading.value = false
    schemaLoaded.value = true
  }
}

function toggleParamsExpanded() {
  paramsExpanded.value = !paramsExpanded.value
}

watch(
  () => [props.binding.modelName, props.models] as const,
  ([name], oldValue) => {
    const prevName = oldValue?.[0]
    if (!name?.trim()) {
      props.binding.params = {}
      schemaFields.value = []
      schemaLoaded.value = false
      schemaError.value = ''
      paramsExpanded.value = false
      return
    }
    if (!resolveProvider()) return
    const resetParams = Boolean(prevName?.trim()) && name !== prevName
    void loadSchema(resetParams)
  },
  { immediate: true, deep: true },
)
</script>

<template>
  <!-- 查看模式 -->
  <div v-if="viewMode" class="agent-view-model-item">
    <span class="agent-view-model-key">{{ label }}</span>
    <span class="agent-view-model-val" :class="{ 'is-empty': !binding.modelName }">
      {{ displayText }}
    </span>
  </div>

  <!-- 编辑模式 -->
  <div
    v-else
    class="agent-model-binding-field"
    :class="[
      `ui-${uiContext}`,
      fieldClass || 'agent-form-field',
      {
        'is-filled': !!binding.modelName,
        'is-muted': disabled,
        'is-params-expanded': paramsExpanded,
      },
    ]"
  >
    <label :class="isWizardField ? 'agent-field-label' : 'agent-form-label'">
      {{ label }}
      <slot name="label-note" />
    </label>

    <div class="binding-model-row">
      <select
        v-model="binding.modelName"
        class="form-input binding-model-select"
        :disabled="disabled"
        :title="selectedModel ? `${selectedModel.modelName} · ${selectedModel.modelProvider}` : undefined"
      >
        <option value="">{{ optional ? emptyOptionLabel : '选择模型…' }}</option>
        <option
          v-for="model in models"
          :key="`${modelTypeKey}-${model.modelName}`"
          :value="model.modelName"
          :title="`${model.modelName} · ${model.modelProvider}`"
        >
          {{ model.modelName }}
        </option>
      </select>

      <button
        v-if="showParamToggle"
        type="button"
        class="param-expand-btn"
        :disabled="disabled || schemaLoading"
        :aria-expanded="paramsExpanded"
        @click="toggleParamsExpanded"
      >
        <svg
          class="param-expand-icon"
          :class="{ 'is-expanded': paramsExpanded }"
          width="10"
          height="10"
          viewBox="0 0 10 10"
          fill="none"
          aria-hidden="true"
        >
          <path d="M2.5 3.5L5 6.5L7.5 3.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        {{ paramsExpanded ? '收起' : '参数' }}
        <span v-if="configuredCount > 0" class="param-expand-badge">{{ configuredCount }}</span>
      </button>
    </div>

    <p v-if="binding.modelName && schemaLoading" class="binding-param-hint">正在加载可调参数…</p>
    <p v-else-if="binding.modelName && schemaError" class="binding-param-error">{{ schemaError }}</p>
    <ModelParamFields
      v-else-if="paramsExpanded && schemaFields.length > 0"
      :fields="schemaFields"
      :model-value="binding.params"
      :disabled="disabled"
      @update:model-value="binding.params = $event"
    />
    <p v-else-if="binding.modelName && schemaLoaded && schemaFields.length === 0" class="binding-param-hint">
      此模型类型暂无可调参数
    </p>
  </div>
</template>

<style scoped>
.agent-model-binding-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  min-width: 0;
}

.agent-model-binding-field.is-muted {
  opacity: 0.72;
}

.agent-form-label,
.agent-field-label {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.3rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.binding-model-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  min-width: 0;
}

.binding-model-select.form-input {
  flex: 1;
  min-width: 0;
  width: auto;
  max-width: none;
}

/* 设置页：独立 Agent 配置，控件更大更易点 */
.ui-settings .binding-model-select.form-input {
  height: 2.125rem;
  padding: 0.4375rem 2rem 0.4375rem 0.75rem;
  font-size: 0.875rem;
  border-radius: 8px;
}

.ui-settings .param-expand-btn {
  height: 2.125rem;
  padding: 0 0.75rem;
  font-size: 0.8125rem;
  border-radius: 8px;
}

.ui-settings .param-expand-badge {
  min-width: 1rem;
  height: 1rem;
  font-size: 0.625rem;
}

/* 聊天侧栏：紧凑但仍可读，一行两个模型+参数 */
.ui-chat .binding-model-select.form-input {
  height: 1.875rem;
  padding: 0.3125rem 1.625rem 0.3125rem 0.5625rem;
  font-size: 0.8125rem;
  border-radius: 6px;
  background-position: right 0.45rem center;
  background-size: 10px 10px;
}

.ui-chat .param-expand-btn {
  height: 1.875rem;
  padding: 0 0.5625rem;
  font-size: 0.75rem;
  border-radius: 6px;
}

.ui-chat .param-expand-badge {
  min-width: 0.875rem;
  height: 0.875rem;
  font-size: 0.5625rem;
}

.param-expand-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.3125rem;
  font-weight: 500;
  white-space: nowrap;
  color: var(--color-text-secondary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease;
}

.param-expand-btn:hover:not(:disabled) {
  color: var(--color-text-accent);
  border-color: color-mix(in srgb, var(--color-text-accent) 35%, var(--color-border));
}

.param-expand-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.param-expand-icon {
  transition: transform 0.15s ease;
}

.param-expand-icon.is-expanded {
  transform: rotate(180deg);
}

.param-expand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.1875rem;
  font-weight: 600;
  color: #fff;
  background: var(--color-text-accent);
  border-radius: 999px;
}

.binding-param-hint {
  margin: 0;
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
}

.binding-param-error {
  margin: 0;
  font-size: 0.6875rem;
  color: var(--color-error, #ef4444);
}
</style>
