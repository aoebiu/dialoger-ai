<template>
  <aside class="agent-settings-panel">
    <nav class="agent-settings-nav">
      <div class="agent-settings-nav-head">
        <h3 class="agent-settings-nav-title">智能体设置</h3>
        <button type="button" class="agent-settings-close" title="关闭" @click="emit('close')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <div v-if="loading" class="agent-settings-loading">加载中…</div>
      <div v-else class="agent-settings-list">
        <button
          v-for="item in agentList"
          :key="item.id"
          type="button"
          class="agent-settings-item"
          :class="{ 'is-active': selectedId === item.id }"
          @click="selectAgent(item.id)"
        >
          <span class="agent-settings-item-name">{{ item.name }}</span>
          <span class="agent-settings-item-meta">
            <span class="agent-settings-flag" :class="{ 'is-on': item.enabled }">
              {{ item.enabled ? '启用' : '禁用' }}
            </span>
          </span>
        </button>
        <p v-if="agentList.length === 0" class="agent-settings-empty">暂无 Agent 配置</p>
      </div>
    </nav>

    <div class="agent-settings-editor">
      <AgentOptionDetailView
        v-if="selectedId != null"
        :key="editorKey"
        embedded
        :option-id="selectedId"
        :initial-item="selectedAgent"
        :creating="false"
        @saved="handleSaved"
        @close="handleEditorClose"
      />
      <div v-else class="agent-settings-placeholder">
        <p>选择左侧配置进行编辑</p>
        <p class="agent-settings-placeholder-hint">新建 Agent 请前往设置页</p>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AgentOptionDetailView from '@/views/AgentOptionDetailView.vue'
import { getAgentOptionList } from '@/api/agentOption'
import type { AgentOptionItem } from '@/api/agentOption'
import { toastError } from '@/utils/toast'

const props = defineProps<{
  initialAgentId?: number | null
}>()

const emit = defineEmits<{
  close: []
  saved: [item: AgentOptionItem]
}>()

const loading = ref(false)
const agentList = ref<AgentOptionItem[]>([])
const selectedId = ref<number | null>(null)

const editorKey = computed(() => `edit-${selectedId.value}`)

const selectedAgent = computed(() =>
  agentList.value.find((item) => item.id === selectedId.value) ?? null,
)

async function loadList() {
  loading.value = true
  try {
    const res = await getAgentOptionList()
    if (res.success && Array.isArray(res.data)) {
      agentList.value = res.data
      if (selectedId.value != null && !res.data.some((item) => item.id === selectedId.value)) {
        selectedId.value = res.data[0]?.id ?? null
      }
    }
  } catch (e: unknown) {
    toastError(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function selectAgent(id: number) {
  selectedId.value = id
}

function handleSaved(item: AgentOptionItem) {
  selectedId.value = item.id
  loadList()
  emit('saved', item)
}

function handleEditorClose() {
  // 嵌入编辑器无独立关闭流程，保留接口以兼容 AgentOptionDetailView
}

onMounted(async () => {
  await loadList()
  if (props.initialAgentId != null && agentList.value.some((item) => item.id === props.initialAgentId)) {
    selectedId.value = props.initialAgentId
  } else if (agentList.value.length > 0) {
    selectedId.value = agentList.value[0]?.id ?? null
  }
})

defineExpose({ loadList })
</script>

<style scoped>
.agent-settings-panel {
  display: flex;
  width: min(720px, 52vw);
  min-width: 480px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.agent-settings-nav {
  width: 200px;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-page);
}

.agent-settings-nav-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 0.75rem 0.625rem;
}

.agent-settings-nav-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.agent-settings-close {
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
  transition: background 0.15s, color 0.15s;
}

.agent-settings-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-card);
}

.agent-settings-loading {
  padding: 1rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.agent-settings-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0.375rem 0.5rem 0.5rem;
}

.agent-settings-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  width: 100%;
  padding: 0.5625rem 0.625rem;
  margin-bottom: 2px;
  font-size: 0.8125rem;
  text-align: left;
  color: var(--color-text-primary);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.agent-settings-item:hover {
  background: var(--color-bg-card);
}

.agent-settings-item.is-active {
  background: var(--color-bg-card);
  box-shadow: inset 2px 0 0 var(--color-text-accent);
}

.agent-settings-item-name {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.agent-settings-item-meta {
  display: flex;
  gap: 0.375rem;
}

.agent-settings-flag {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
}

.agent-settings-flag.is-on {
  color: var(--color-text-accent);
}

.agent-settings-empty {
  margin: 0;
  padding: 1rem 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  text-align: center;
  line-height: 1.5;
}

.agent-settings-editor {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.agent-settings-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 2rem;
  text-align: center;
}

.agent-settings-placeholder p {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.agent-settings-placeholder-hint {
  font-size: 0.8125rem !important;
  color: var(--color-text-tertiary) !important;
}

@media (max-width: 1100px) {
  .agent-settings-panel {
    width: min(640px, 58vw);
    min-width: 400px;
  }

  .agent-settings-nav {
    width: 168px;
    min-width: 168px;
  }
}
</style>
