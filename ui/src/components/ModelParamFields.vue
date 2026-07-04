<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import type { ModelParamFieldSchema } from '@/api/model'
import { formatFieldHelp, formatFieldPlaceholder } from '@/utils/modelParams'

const props = defineProps<{
  fields: ModelParamFieldSchema[]
  modelValue: Record<string, unknown>
  disabled?: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [Record<string, unknown>]
}>()

interface TipState {
  text: string
  x: number
  y: number
  placement: 'top' | 'bottom'
}

const activeTip = ref<TipState | null>(null)

function showTip(event: FocusEvent | MouseEvent, text: string) {
  const el = event.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const placement: 'top' | 'bottom' = rect.top > 140 ? 'top' : 'bottom'
  activeTip.value = {
    text,
    x: rect.left + rect.width / 2,
    y: placement === 'top' ? rect.top - 8 : rect.bottom + 8,
    placement,
  }
}

function hideTip() {
  activeTip.value = null
}

onMounted(() => {
  window.addEventListener('scroll', hideTip, true)
  window.addEventListener('resize', hideTip)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', hideTip, true)
  window.removeEventListener('resize', hideTip)
})

function updateField(name: string, value: unknown) {
  const next = { ...props.modelValue }
  if (value === null || value === undefined || value === '') {
    delete next[name]
  } else {
    next[name] = value
  }
  emit('update:modelValue', next)
}

function stringArrayText(name: string): string {
  const value = props.modelValue[name]
  if (!Array.isArray(value)) return ''
  return value.map(String).join('\n')
}

function updateStringArray(name: string, raw: string) {
  const lines = raw
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  updateField(name, lines.length > 0 ? lines : null)
}

function inputAttrs(field: ModelParamFieldSchema): Record<string, string | number | undefined> {
  const attrs: Record<string, string | number | undefined> = {}
  if (field.min != null) attrs.min = field.min
  if (field.max != null) attrs.max = field.max
  return attrs
}

function boolSelectValue(name: string): string {
  const value = props.modelValue[name]
  if (value === true) return 'true'
  if (value === false) return 'false'
  return ''
}

function onBoolChange(name: string, raw: string) {
  if (raw === '') updateField(name, null)
  else updateField(name, raw === 'true')
}

function isWideField(field: ModelParamFieldSchema): boolean {
  return field.type === 'STRING_ARRAY'
    || field.type === 'STRING'
    || (field.type === 'ENUM' && (field.enumValues?.length ?? 0) > 4)
}

const hasFields = computed(() => props.fields.length > 0)
</script>

<template>
  <div v-if="loading" class="model-param-fields model-param-fields--loading">
    <span class="model-param-loading-dot" />
    加载参数…
  </div>
  <div v-else-if="hasFields" class="model-param-fields">
    <div class="model-param-grid">
      <div
        v-for="field in fields"
        :key="field.name"
        class="model-param-field"
        :class="{ 'model-param-field--wide': isWideField(field) }"
      >
        <div class="model-param-label-row">
          <label class="model-param-label">
            <span class="model-param-name">{{ field.name }}</span>
            <span v-if="field.required" class="model-param-required">*</span>
          </label>
          <span
            class="model-param-info"
            tabindex="0"
            role="button"
            :aria-label="formatFieldHelp(field)"
            @mouseenter="showTip($event, formatFieldHelp(field))"
            @mouseleave="hideTip"
            @focus="showTip($event, formatFieldHelp(field))"
            @blur="hideTip"
          >
            <svg class="model-param-info-icon" width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
              <circle cx="6" cy="6" r="5" stroke="currentColor" stroke-width="1.1"/>
              <path d="M6 5.2V8.2M6 3.6h.01" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
          </span>
        </div>

        <select
          v-if="field.type === 'ENUM'"
          class="form-input model-param-input"
          :disabled="disabled"
          :value="modelValue[field.name] ?? ''"
          @change="updateField(field.name, ($event.target as HTMLSelectElement).value || null)"
        >
          <option value="">默认</option>
          <option v-for="opt in field.enumValues ?? []" :key="opt" :value="opt">{{ opt }}</option>
        </select>

        <select
          v-else-if="field.type === 'BOOLEAN'"
          class="form-input model-param-input"
          :disabled="disabled"
          :value="boolSelectValue(field.name)"
          @change="onBoolChange(field.name, ($event.target as HTMLSelectElement).value)"
        >
          <option value="">默认</option>
          <option value="true">开</option>
          <option value="false">关</option>
        </select>

        <textarea
          v-else-if="field.type === 'STRING_ARRAY'"
          class="form-input form-textarea model-param-input model-param-textarea"
          rows="2"
          :disabled="disabled"
          :placeholder="formatFieldPlaceholder(field)"
          :value="stringArrayText(field.name)"
          @input="updateStringArray(field.name, ($event.target as HTMLTextAreaElement).value)"
        />

        <input
          v-else-if="field.type === 'INTEGER'"
          type="number"
          step="1"
          class="form-input model-param-input"
          :disabled="disabled"
          v-bind="inputAttrs(field)"
          :placeholder="formatFieldPlaceholder(field)"
          :value="modelValue[field.name] ?? ''"
          @input="updateField(field.name, ($event.target as HTMLInputElement).value === '' ? null : Number(($event.target as HTMLInputElement).value))"
        />

        <input
          v-else-if="field.type === 'FLOAT' || field.type === 'DOUBLE'"
          type="number"
          step="any"
          class="form-input model-param-input"
          :disabled="disabled"
          v-bind="inputAttrs(field)"
          :placeholder="formatFieldPlaceholder(field)"
          :value="modelValue[field.name] ?? ''"
          @input="updateField(field.name, ($event.target as HTMLInputElement).value === '' ? null : Number(($event.target as HTMLInputElement).value))"
        />

        <input
          v-else
          type="text"
          class="form-input model-param-input"
          :disabled="disabled"
          :placeholder="formatFieldPlaceholder(field)"
          :value="modelValue[field.name] ?? ''"
          @input="updateField(field.name, ($event.target as HTMLInputElement).value || null)"
        />
      </div>
    </div>
  </div>

  <Teleport to="body">
    <div
      v-if="activeTip"
      class="model-param-tip"
      :class="activeTip.placement === 'top' ? 'model-param-tip--top' : 'model-param-tip--bottom'"
      :style="{ left: `${activeTip.x}px`, top: `${activeTip.y}px` }"
      role="tooltip"
    >
      {{ activeTip.text }}
    </div>
  </Teleport>
</template>

<style scoped>
.model-param-fields {
  padding: 0.5rem 0.625rem;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.model-param-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.375rem 0.5rem;
}

.model-param-field {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.model-param-field--wide {
  grid-column: 1 / -1;
}

.model-param-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.25rem;
  min-width: 0;
}

.model-param-label {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  min-width: 0;
}

.model-param-name {
  font-size: 0.6875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-param-required {
  flex-shrink: 0;
  font-size: 0.625rem;
  color: var(--color-error, #ef4444);
}

.model-param-info {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  color: var(--color-text-tertiary);
  cursor: help;
  border-radius: 50%;
  outline: none;
}

.model-param-info:focus-visible {
  color: var(--color-text-accent);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-text-accent) 25%, transparent);
}

.model-param-info-icon {
  display: block;
}

.model-param-input {
  padding: 0.3125rem 0.5rem;
  font-size: 0.8125rem;
  border-radius: 6px;
}

select.model-param-input {
  padding-right: 1.75rem;
  background-position: right 0.4rem center;
}

.model-param-textarea {
  min-height: 2.25rem;
  line-height: 1.35;
}

.model-param-fields--loading {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

.model-param-loading-dot {
  width: 0.375rem;
  height: 0.375rem;
  border-radius: 50%;
  background: var(--color-text-accent);
  animation: model-param-pulse 1s ease-in-out infinite;
}

@keyframes model-param-pulse {
  0%, 100% { opacity: 0.35; }
  50% { opacity: 1; }
}

@media (max-width: 900px) {
  .model-param-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .model-param-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
.model-param-tip {
  position: fixed;
  z-index: 10000;
  width: max-content;
  max-width: min(260px, calc(100vw - 24px));
  padding: 0.5rem 0.625rem;
  font-size: 0.6875rem;
  font-weight: 400;
  line-height: 1.45;
  color: var(--color-text-primary);
  white-space: pre-line;
  pointer-events: none;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
}

.model-param-tip--top {
  transform: translate(-50%, -100%);
}

.model-param-tip--bottom {
  transform: translate(-50%, 0);
}
</style>
