<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ToolExecution } from '@/stores/conversation'

const props = defineProps<{
  execution: ToolExecution | null
}>()

const emit = defineEmits<{
  close: []
}>()

type TabKey = 'input' | 'output'

const activeTab = ref<TabKey>('input')

watch(
  () => props.execution,
  () => {
    activeTab.value = 'input'
  }
)

function formatJson(raw?: string): string {
  if (!raw?.trim()) return '—'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="execution" class="tool-modal-overlay" @click.self="emit('close')">
      <div class="tool-modal" role="dialog" aria-modal="true">
        <header class="tool-modal-header">
          <span class="tool-modal-title">工具调用：{{ execution.toolName }}</span>
          <button type="button" class="tool-modal-close" aria-label="关闭" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </header>

        <div class="tool-modal-tabs">
          <button
            type="button"
            class="tool-modal-tab"
            :class="{ active: activeTab === 'input' }"
            @click="activeTab = 'input'"
          >
            调用参数
          </button>
          <button
            type="button"
            class="tool-modal-tab"
            :class="{ active: activeTab === 'output' }"
            @click="activeTab = 'output'"
          >
            执行结果
          </button>
        </div>

        <div class="tool-modal-body">
          <pre v-if="activeTab === 'input'" class="tool-json">{{ formatJson(execution.arguments) }}</pre>
          <pre v-else class="tool-json">{{ formatJson(execution.result) }}</pre>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.tool-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.45);
}

.tool-modal {
  width: min(720px, 100%);
  max-height: min(80vh, 800px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  background: var(--color-bg-card);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}

.tool-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1.1rem;
  border-bottom: 1px solid var(--color-border);
}

.tool-modal-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-modal-close {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.tool-modal-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.tool-modal-tabs {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1.1rem 0;
}

.tool-modal-tab {
  padding: 0.35rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
}

.tool-modal-tab:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.tool-modal-tab.active {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border-color: var(--color-border);
}

.tool-modal-body {
  flex: 1;
  min-height: 0;
  padding: 0.85rem 1.1rem 1.1rem;
}

.tool-json {
  margin: 0;
  padding: 0.85rem 1rem;
  max-height: calc(80vh - 140px);
  overflow: auto;
  font-size: 0.8125rem;
  line-height: 1.5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
