<template>
  <div class="chat-page">
    <!-- 顶栏 -->
    <header class="top-nav">
      <div class="top-nav-left">
        <div class="brand">
          <span class="brand-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </span>
          <span class="brand-name">Dialoger AI</span>
        </div>
        <template v-if="currentTitle">
          <span class="nav-sep">/</span>
          <span class="nav-title">{{ currentTitle }}</span>
        </template>
      </div>
      <div class="top-nav-right">
        <div v-if="enabledAgentOptions.length > 0" class="agent-picker">
          <select
            id="agent-option-select"
            v-model.number="selectedAgentOptionId"
            class="agent-select"
            :disabled="streaming"
          >
            <option v-for="opt in enabledAgentOptions" :key="opt.id" :value="opt.id">
              {{ opt.name }}
            </option>
          </select>
        </div>
        <button
          type="button"
          class="nav-icon-btn"
          :title="themeTooltip"
          @click="theme.toggleTheme()"
        >
          <svg v-if="theme.resolvedTheme === 'dark'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
        </button>
        <button type="button" class="nav-icon-btn" title="后台配置" @click="goToSettings">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        </button>
        <div class="user-chip">
          <span class="user-avatar">{{ userInitial }}</span>
          <span class="user-name">{{ auth.user?.nickname || auth.user?.username }}</span>
        </div>
        <button type="button" class="nav-text-btn" @click="handleLogout">退出</button>
      </div>
    </header>

    <div class="chat-layout">
      <!-- 左侧会话列表 -->
      <aside class="session-panel">
        <div class="session-panel-head">
          <h2 class="session-panel-title">对话列表</h2>
          <button type="button" class="new-chat-btn" :disabled="creating" @click="handleNewChat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            {{ creating ? '创建中…' : '新建' }}
          </button>
        </div>
        <nav class="conversation-list">
          <div v-for="key in GROUP_KEYS" :key="key">
            <template v-if="groupedItems(key).length > 0">
              <div class="conv-group-label">{{ GROUP_LABELS[key] }}</div>
              <div
                v-for="c in groupedItems(key)"
                :key="c.id"
                class="conversation-item"
                :class="{ active: conv.currentId === c.id }"
                @click="handleSelectConversation(c.id)"
              >
                <span class="conversation-title">{{ c.title }}</span>
                <button
                  type="button"
                  class="conversation-delete-btn"
                  title="删除对话"
                  @click.stop="handleDeleteConversation(c.id)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
                </button>
              </div>
            </template>
          </div>
          <p v-if="conv.conversations.length === 0" class="no-conversations">
            暂无对话，点击「新建」开始
          </p>
        </nav>
      </aside>

      <!-- 主对话区 -->
      <main class="chat-stage">
        <div v-if="!conv.currentId" class="stage-empty">
          <div class="stage-empty-icon" aria-hidden="true">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </div>
          <p class="stage-empty-title">选择或新建一个对话</p>
          <p class="stage-empty-desc">从左侧选择历史对话，或点击「新建」开始与 AI 聊天</p>
          <button type="button" class="stage-empty-btn" :disabled="creating" @click="handleNewChat">
            {{ creating ? '创建中…' : '新建对话' }}
          </button>
        </div>

        <div v-else class="chat-panel">
          <div class="panel-toolbar">
            <span class="panel-title">对话测试</span>
            <div class="panel-toolbar-actions">
              <button
                type="button"
                class="toolbar-btn"
                :class="{ 'is-active': agentPanelOpen }"
                title="智能体设置"
                @click="toggleAgentPanel"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M12 8V4H8"/>
                  <rect x="4" y="8" width="16" height="12" rx="2"/>
                  <path d="M2 14h2"/>
                  <path d="M20 14h2"/>
                  <line x1="9" y1="13" x2="9" y2="15"/>
                  <line x1="15" y1="13" x2="15" y2="15"/>
                </svg>
              </button>
            </div>
          </div>

          <div ref="messagesEl" class="panel-body">
            <div v-if="loadingHistory" class="panel-status">加载对话记录中…</div>

            <div v-else-if="conv.currentMessages.length === 0" class="welcome-state">
              <div class="welcome-avatar" aria-hidden="true">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </div>
              <h3 class="welcome-name">{{ currentAgentName }}</h3>
              <p class="welcome-hint">在这里直接对话，体验智能助手效果</p>
              <div class="welcome-bubble">
                <p>发送一条消息开始对话</p>
              </div>
            </div>

            <div v-else class="messages">
              <div
                v-for="(m, i) in conv.currentMessages"
                :key="m.id ?? `${conv.currentId ?? 'session'}-${i}`"
                class="message"
                :class="m.role"
              >
                <div v-if="editingIndex === i" class="edit-area">
                  <textarea v-model="editText" class="edit-textarea" rows="4" />
                  <div class="edit-actions">
                    <button type="button" class="edit-btn cancel" @click="handleCancelEdit">取消</button>
                    <button type="button" class="edit-btn save" @click="handleSaveEdit(i)">发送</button>
                  </div>
                </div>
                <template v-else>
                  <div v-if="m.role === 'assistant'" class="msg-avatar" aria-hidden="true">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  </div>
                  <div class="msg-content">
                    <div class="bubble">
                      <div class="content markdown-body" v-html="renderMarkdown(m.content)"></div>
                    </div>
                    <div v-if="m.role === 'assistant' && m.ragSources?.length" class="rag-badges">
                      <button
                        v-for="kbName in uniqueKbNames(ragSourcesOf(m))"
                        :key="kbName"
                        type="button"
                        class="rag-badge"
                        @click="handleRagBadgeClick(ragSourcesOf(m), kbName)"
                      >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                        {{ kbName }}
                      </button>
                    </div>
                    <div v-if="m.role === 'assistant' && m.toolExecutions?.length" class="tool-badges">
                      <button
                        v-for="(exec, ei) in m.toolExecutions"
                        :key="exec.id ?? `${exec.toolName}-${ei}`"
                        type="button"
                        class="tool-badge"
                        @click="handleToolBadgeClick(exec)"
                      >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
                        {{ exec.toolName }}
                      </button>
                    </div>
                    <div v-if="m.role === 'user' && !streaming" class="message-actions">
                      <button type="button" class="action-btn" title="重新生成" @click="handleRegenerate(i)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
                      </button>
                      <button type="button" class="action-btn" title="修改" @click="handleStartEdit(i)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                      </button>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <div class="panel-footer">
            <div class="input-box" :class="{ disabled: streaming }">
              <textarea
                v-model="inputText"
                class="input-field"
                placeholder="发消息"
                rows="1"
                :disabled="streaming"
                @keydown.enter.exact.prevent="send"
                @input="autoResizeInput"
              />
              <button
                type="button"
                class="input-send-btn"
                :disabled="!inputText.trim() || streaming"
                :title="streaming ? '回复中…' : '发送'"
                @click="send"
              >
                <svg v-if="streaming" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
              </button>
            </div>
            <p class="input-disclaimer">内容由 AI 生成，请仔细甄别</p>
          </div>
        </div>
      </main>

      <AgentSettingsPanel
        v-if="agentPanelOpen"
        :initial-agent-id="selectedAgentOptionId"
        @close="agentPanelOpen = false"
        @saved="handleAgentSaved"
      />
    </div>

    <Teleport to="body">
      <div v-if="ragModal" class="rag-modal-overlay" @click.self="ragModal = null">
        <div class="rag-modal">
          <div class="rag-modal-header">
            <span>引用的知识库片段</span>
            <button type="button" class="rag-modal-close" @click="ragModal = null">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="rag-modal-body">
            <div v-for="(src, si) in ragModal" :key="si" class="rag-source-item">
              <div class="rag-source-kb">{{ src.kbName || src.indexName || '知识库' }}</div>
              <div class="rag-source-text">{{ src.text }}</div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <ToolExecutionModal :execution="toolModal" @close="toolModal = null" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import ToolExecutionModal from '@/components/ToolExecutionModal.vue'
import AgentSettingsPanel from '@/components/AgentSettingsPanel.vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useConversationStore } from '@/stores/conversation'
import type { RagSource, ToolExecution } from '@/stores/conversation'
import { marked } from 'marked'
import hljs from 'highlight.js/lib/common'
import {
  createChat,
  chatStream,
  getHistory,
  getSessions,
  deleteSession,
  getRagSources,
  getToolExecutions,
} from '@/api/chat'
import type { HistoryMessage, HistoryData, HistoryResponse, SessionItem } from '@/api/chat'
import { getAgentOptionList } from '@/api/agentOption'
import type { AgentOptionItem } from '@/api/agentOption'

const AGENT_OPTION_STORAGE_KEY = 'dialoger:agentOptionId'

marked.setOptions({ gfm: true, breaks: true })

const COPY_BTN = `<button class="copy-code-btn" type="button" title="复制代码"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg></button>`

const renderer = new marked.Renderer()
renderer.code = function ({ text, lang }: { text: string; lang?: string }) {
  let highlighted: string
  let langClass: string
  if (lang && hljs.getLanguage(lang)) {
    highlighted = hljs.highlight(text, { language: lang }).value
    langClass = `hljs language-${lang}`
  } else {
    highlighted = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    langClass = 'hljs'
  }
  return `<div class="code-block-wrapper"><pre><code class="${langClass}">${highlighted}</code></pre>${COPY_BTN}</div>`
}

function completeMarkdown(text: string): string {
  const lines = text.split('\n')
  let inCodeBlock = false
  for (const line of lines) {
    if (/^(`{3,}|~{3,})/.test(line.trimStart())) {
      inCodeBlock = !inCodeBlock
    }
  }
  return inCodeBlock ? text + '\n```' : text
}

function renderMarkdown(text: string): string {
  if (!text?.trim()) return ''
  try {
    const normalized = completeMarkdown(text.trim())
    const cached = markdownCache.get(normalized)
    if (cached) return cached
    const rawHtml = marked.parse(normalized, { async: false, renderer }) as string
    const safeHtml = sanitizeMarkdownHtml(rawHtml)
    markdownCache.set(normalized, safeHtml)
    return safeHtml
  } catch {
    return text
  }
}

function sanitizeMarkdownHtml(rawHtml: string): string {
  const parser = new DOMParser()
  const doc = parser.parseFromString(rawHtml, 'text/html')
  doc.querySelectorAll('script,style,iframe,object,embed,link,meta').forEach((el) => el.remove())
  doc.body.querySelectorAll('*').forEach((element) => {
    for (let i = element.attributes.length - 1; i >= 0; i -= 1) {
      const attr = element.attributes.item(i)
      if (!attr) continue
      const name = attr.name.toLowerCase()
      const value = attr.value.trim().toLowerCase()
      if (name.startsWith('on')) {
        element.removeAttribute(attr.name)
        continue
      }
      if ((name === 'href' || name === 'src') && value.startsWith('javascript:')) {
        element.removeAttribute(attr.name)
      }
    }
  })
  return doc.body.innerHTML
}

function handleCopyCode(e: MouseEvent) {
  const btn = (e.target as HTMLElement).closest('.copy-code-btn') as HTMLButtonElement | null
  if (!btn) return
  const wrapper = btn.closest('.code-block-wrapper')
  const code = wrapper?.querySelector('code')
  if (!code) return
  navigator.clipboard.writeText(code.textContent || '').then(() => {
    btn.classList.add('copied')
    btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>'
    setTimeout(() => {
      btn.classList.remove('copied')
      btn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'
    }, 2000)
  })
}

onMounted(() => {
  document.addEventListener('click', handleCopyCode)
})
onUnmounted(() => {
  document.removeEventListener('click', handleCopyCode)
  abortStream?.()
  abortStream = null
  stopTypewriter()
})

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const conv = useConversationStore()

const themeTooltip = computed(() =>
  theme.resolvedTheme === 'dark' ? '切换到浅色模式' : '切换到深色模式'
)

const currentTitle = computed(() =>
  conv.conversations.find(c => c.id === conv.currentId)?.title ?? ''
)

const userInitial = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || '?'
  return name.charAt(0).toUpperCase()
})

const currentAgentName = computed(() => {
  const id = selectedAgentOptionId.value
  if (id != null) {
    const opt = enabledAgentOptions.value.find((item) => item.id === id)
    if (opt?.name) return opt.name
  }
  return '智能助手'
})

const messagesEl = ref<HTMLElement | null>(null)

const GROUP_KEYS = ['today', 'week', 'month', 'older'] as const
type GroupKey = (typeof GROUP_KEYS)[number]
const GROUP_LABELS: Record<GroupKey, string> = {
  today: '今天', week: '本周', month: '本月', older: '更久',
}
const markdownCache = new Map<string, string>()

const groupedConversations = computed(() => {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const dow = startOfToday.getDay()
  const startOfWeek = new Date(startOfToday)
  startOfWeek.setDate(startOfToday.getDate() - (dow === 0 ? 6 : dow - 1))
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)

  const groups: Record<GroupKey, typeof conv.conversations> = {
    today: [],
    week: [],
    month: [],
    older: [],
  }
  for (const c of conv.conversations) {
    const d = c.updatedAt ? new Date(c.updatedAt) : null
    if (!d || isNaN(d.getTime())) { groups.older.push(c); continue }
    if (d >= startOfToday)      groups.today.push(c)
    else if (d >= startOfWeek)  groups.week.push(c)
    else if (d >= startOfMonth) groups.month.push(c)
    else                        groups.older.push(c)
  }
  return groups
})

function groupedItems(key: GroupKey) {
  return groupedConversations.value[key]
}

const inputText = ref('')
const streaming = ref(false)
const creating = ref(false)
const loadingHistory = ref(false)
const editingIndex = ref<number | null>(null)
const editText = ref('')
const ragModal = ref<RagSource[] | null>(null)
const toolModal = ref<ToolExecution | null>(null)
const agentOptionList = ref<AgentOptionItem[]>([])
const selectedAgentOptionId = ref<number | null>(null)
const agentPanelOpen = ref(false)
let abortStream: (() => void) | null = null

const enabledAgentOptions = computed(() =>
  agentOptionList.value.filter((item) => item.enabled !== false),
)

function resolveSelectedAgentOptionId(options: AgentOptionItem[]): number | null {
  if (options.length === 0) return null
  const stored = localStorage.getItem(AGENT_OPTION_STORAGE_KEY)
  const storedId = stored ? Number(stored) : NaN
  if (Number.isFinite(storedId) && options.some((item) => item.id === storedId)) {
    return storedId
  }
  return options[0]?.id ?? null
}

function getCurrentOptionId(): number | undefined {
  return selectedAgentOptionId.value ?? undefined
}

async function loadAgentOptions() {
  try {
    const res = await getAgentOptionList()
    if (res.success && Array.isArray(res.data)) {
      agentOptionList.value = res.data
      selectedAgentOptionId.value = resolveSelectedAgentOptionId(enabledAgentOptions.value)
    }
  } catch {
    // 忽略
  }
}

watch(selectedAgentOptionId, (id) => {
  if (id != null) {
    localStorage.setItem(AGENT_OPTION_STORAGE_KEY, String(id))
  }
})

watch(
  () => conv.currentMessages.length,
  () => {
    nextTick(() => {
      const el = messagesEl.value
      if (el) el.scrollTop = el.scrollHeight
    })
  },
)

// 打字机效果：后端 chunk 先入缓冲，再逐字显示；缓冲多时加快间隔
let typewriterBuffer = ''
let typewriterTimerId: ReturnType<typeof setTimeout> | null = null
let streamSessionId: string | null = null
const TYPEWRITER_BASE_MS = 48
const TYPEWRITER_FAST_MS = 16
const TYPEWRITER_FAST_THRESHOLD = 12

function stopTypewriter() {
  if (typewriterTimerId !== null) {
    clearTimeout(typewriterTimerId)
    typewriterTimerId = null
  }
  typewriterBuffer = ''
  streamSessionId = null
}

function flushOneChar() {
  const sid = streamSessionId
  if (!sid || typewriterBuffer === '') return
  const list = conv.currentMessages
  const last = list[list.length - 1]
  if (last?.role !== 'assistant') return
  const chars = [...typewriterBuffer]
  const first = chars[0]
  if (!first) return
  typewriterBuffer = chars.slice(1).join('')
  conv.updateLastMessage(sid, last.content + first)

  if (typewriterBuffer === '' && !streaming.value) {
    typewriterTimerId = null
    const sid = streamSessionId
    streamSessionId = null
    if (sid) {
      const messages = conv.currentMessages
      if (messages.length === 2 && messages[1]?.role === 'assistant') {
        const assistantIdx = messages.length - 1
        conv.fetchConversationTitle(sid).then((meta) => {
          patchAssistantMeta(sid, assistantIdx, meta?.sourceIds, meta?.toolExecutionIds)
        })
      }
    }
    return
  }
  const ms = typewriterBuffer.length > TYPEWRITER_FAST_THRESHOLD ? TYPEWRITER_FAST_MS : TYPEWRITER_BASE_MS
  typewriterTimerId = setTimeout(flushOneChar, ms)
}

function startTypewriterIfNeeded() {
  if (typewriterTimerId !== null || typewriterBuffer === '' || !streamSessionId) return
  const ms = typewriterBuffer.length > TYPEWRITER_FAST_THRESHOLD ? TYPEWRITER_FAST_MS : TYPEWRITER_BASE_MS
  typewriterTimerId = setTimeout(flushOneChar, ms)
}

/** 从 getHistory 返回的 data 中解析出消息列表和 rag / tool 映射 */
function parseHistoryMessages(data: HistoryData | undefined): {
  messages: import('@/stores/conversation').MessageItem[]
  ragSourceMap: Record<string, number[]>
  toolExecutionMap: Record<string, number[]>
} {
  const rawList = Array.isArray(data) ? data : ((data as HistoryResponse)?.messages ?? [])
  const ragSourceMap: Record<string, number[]> =
    (!Array.isArray(data) ? (data as HistoryResponse)?.ragSourceMap : undefined) ?? {}
  const toolExecutionMap: Record<string, number[]> =
    (!Array.isArray(data) ? (data as HistoryResponse)?.toolExecutionMap : undefined) ?? {}
  const messages = (rawList as HistoryMessage[])
    .filter((m) => (m.role || '').toUpperCase() !== 'SYSTEM')
    .map((m) => {
      const role = ((m.role || 'user').toUpperCase() === 'USER' ? 'user' : 'assistant') as 'user' | 'assistant'
      return { id: m.id, role, content: m.content ?? '' }
    })
  return { messages, ragSourceMap, toolExecutionMap }
}

/** 从服务端历史记录同步消息 id（本地 append 的消息没有 id，重新生成需要 fromMessageId） */
async function syncMessageIds(sessionId: string) {
  try {
    const res = await getHistory(sessionId)
    if (!res.success || !res.data) return
    const { messages: serverMessages } = parseHistoryMessages(res.data)
    const local = conv.currentMessages
    if (!local.length || !serverMessages.length) return
    const next = [...local]
    let changed = false
    for (let i = 0; i < Math.min(next.length, serverMessages.length); i++) {
      const localMsg = next[i]
      const serverMsg = serverMessages[i]
      if (
        localMsg?.id == null &&
        serverMsg?.id != null &&
        localMsg?.role === serverMsg.role
      ) {
        next[i] = { ...localMsg, id: serverMsg.id }
        changed = true
      }
    }
    if (changed) conv.setMessages(sessionId, next)
  } catch (e) {
    console.error('[ChatView] syncMessageIds failed:', e)
  }
}

/** 批量拉取 rag sources 并按 messageId 挂载到消息列表 */
async function attachRagSources(
  messages: import('@/stores/conversation').MessageItem[],
  ragSourceMap: Record<string, number[]>
) {
  const allIds = Object.values(ragSourceMap).flat()
  if (!allIds.length) return messages
  try {
    const res = await getRagSources(allIds)
    if (!res.success || !res.data?.sources?.length) return messages
    const sourceToMsg: Record<number, number> = {}
    for (const [msgIdStr, ids] of Object.entries(ragSourceMap)) {
      for (const id of ids) sourceToMsg[id] = Number(msgIdStr)
    }
    const msgIdToSources: Record<number, RagSource[]> = {}
    for (const src of res.data.sources) {
      if (src.id == null) continue
      const msgId = sourceToMsg[src.id]
      if (msgId == null) continue
      msgIdToSources[msgId] ??= []
      msgIdToSources[msgId].push(src)
    }
    return messages.map((m) =>
      m.id && msgIdToSources[m.id] ? { ...m, ragSources: msgIdToSources[m.id] } : m
    )
  } catch {
    return messages
  }
}

/** 批量拉取 tool executions 并按 messageId 挂载到消息列表 */
async function attachToolExecutions(
  messages: import('@/stores/conversation').MessageItem[],
  toolExecutionMap: Record<string, number[]>
) {
  const allIds = Object.values(toolExecutionMap).flat()
  if (!allIds.length) return messages
  try {
    const res = await getToolExecutions(allIds)
    if (!res.success || !res.data?.executions?.length) return messages
    const execToMsg: Record<number, number> = {}
    for (const [msgIdStr, ids] of Object.entries(toolExecutionMap)) {
      for (const id of ids) execToMsg[id] = Number(msgIdStr)
    }
    const msgIdToExecutions: Record<number, ToolExecution[]> = {}
    for (const exec of res.data.executions) {
      if (exec.id == null) continue
      const msgId = execToMsg[exec.id]
      if (msgId == null) continue
      msgIdToExecutions[msgId] ??= []
      msgIdToExecutions[msgId].push(exec)
    }
    return messages.map((m) =>
      m.id && msgIdToExecutions[m.id] ? { ...m, toolExecutions: msgIdToExecutions[m.id] } : m
    )
  } catch {
    return messages
  }
}

async function attachMessageMeta(
  sessionId: string,
  messages: import('@/stores/conversation').MessageItem[],
  ragSourceMap: Record<string, number[]>,
  toolExecutionMap: Record<string, number[]>
) {
  let patched = await attachRagSources(messages, ragSourceMap)
  patched = await attachToolExecutions(patched, toolExecutionMap)
  conv.setMessages(sessionId, patched)
}

/** 拉取并挂载单条 assistant 消息的 rag / tool 元数据 */
async function patchAssistantMeta(
  sessionId: string,
  assistantIdx: number,
  sourceIds?: number[],
  toolExecutionIds?: number[]
) {
  const patch: Partial<import('@/stores/conversation').MessageItem> = {}
  try {
    if (sourceIds?.length) {
      const sourcesRes = await getRagSources(sourceIds)
      if (sourcesRes.success && sourcesRes.data?.sources?.length) {
        patch.ragSources = sourcesRes.data.sources
      }
    }
    if (toolExecutionIds?.length) {
      const execRes = await getToolExecutions(toolExecutionIds)
      if (execRes.success && execRes.data?.executions?.length) {
        patch.toolExecutions = execRes.data.executions
      }
    }
    if (Object.keys(patch).length) {
      conv.patchMessage(sessionId, assistantIdx, patch)
    }
  } catch {}
}

/** 非首轮对话完成后拉取最新 assistant 的 rag / tool 元数据 */
async function fetchLatestAssistantMeta(sessionId: string, assistantIdx: number) {
  const meta = await conv.fetchConversationTitle(sessionId).catch(() => undefined)
  await patchAssistantMeta(sessionId, assistantIdx, meta?.sourceIds, meta?.toolExecutionIds)
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.replace('/login')
    return
  }
  // 刷新页面时：先拉取所有对话列表
  try {
    const sessionsRes = await getSessions()
    if (sessionsRes.success && sessionsRes.data && Array.isArray(sessionsRes.data)) {
      conv.setConversations(
        sessionsRes.data.map((s: SessionItem) => ({
          id: s.sessionId ?? s.id ?? '',
          title: s.title || '新对话',
          updatedAt: s.lastModified,
        }))
      )
    }
  } catch (e) {
    console.error('拉取对话列表失败:', e)
  }
  await loadAgentOptions()
  // 恢复当前选中的会话并拉取该对话历史；若持久化 session 不属于当前账号则清空选中态
  const sessionId = conv.getPersistedSessionId()
  if (sessionId && conv.conversations.some((c) => c.id === sessionId)) {
    conv.setCurrent(sessionId)
    try {
      const res = await getHistory(sessionId)
      if (res.success && res.data) {
        const { messages, ragSourceMap, toolExecutionMap } = parseHistoryMessages(res.data)
        conv.setMessages(sessionId, messages)
        await attachMessageMeta(sessionId, messages, ragSourceMap, toolExecutionMap)
      }
    } catch (e) {
      console.error('拉取对话历史失败:', e)
    }
  } else {
    conv.setCurrent(null)
  }
})

async function handleDeleteConversation(sessionId: string) {
  const res = await deleteSession(sessionId)
  if (res.success) {
    conv.removeConversation(sessionId)
  }
}

async function handleNewChat() {
  if (creating.value) return
  creating.value = true
  try {
    const res = await createChat()
    if (res.success && res.data) {
      conv.addConversation(res.data.sessionId, res.data.title || '新对话')
    } else if (res.success && !res.data) {
      // 最近的会话已为空，直接切换过去
      const latest = conv.conversations[0]
      if (latest) conv.setCurrent(latest.id)
    }
  } finally {
    creating.value = false
  }
}

function goToSettings() {
  router.push('/settings')
}

function toggleAgentPanel() {
  agentPanelOpen.value = !agentPanelOpen.value
}

async function handleAgentSaved(item: AgentOptionItem) {
  await loadAgentOptions()
  if (item.enabled !== false) {
    selectedAgentOptionId.value = item.id
  }
}

async function handleLogout() {
  await auth.logout()
  router.replace('/login')
}

/** 点击左侧会话标题：切换当前会话并拉取该会话历史消息渲染 */
async function handleSelectConversation(sessionId: string) {
  conv.setCurrent(sessionId)
  loadingHistory.value = true
  try {
    const res = await getHistory(sessionId)
    if (res.success && res.data) {
      const { messages, ragSourceMap, toolExecutionMap } = parseHistoryMessages(res.data)
      conv.setMessages(sessionId, messages)
      await attachMessageMeta(sessionId, messages, ragSourceMap, toolExecutionMap)
    } else {
      conv.setMessages(sessionId, [])
    }
  } catch (e) {
    console.error('拉取对话历史失败:', e)
    conv.setMessages(sessionId, [])
  } finally {
    loadingHistory.value = false
  }
}

/** 从指定索引截断消息，重新发送 text 并流式获取 AI 回复 */
async function resendFrom(index: number, text: string) {
  const sessionId = conv.currentId
  if (!sessionId || streaming.value) return
  await syncMessageIds(sessionId)
  const fromMessageId = conv.currentMessages[index]?.id
  if (fromMessageId == null) {
    console.warn('[ChatView] fromMessageId is null/undefined, regenerate will not delete messages on server')
  }
  conv.truncateMessagesFrom(sessionId, index)
  conv.appendMessage(sessionId, { role: 'user', content: text })
  conv.appendMessage(sessionId, { role: 'assistant', content: '' })
  const assistantMsgIdx = conv.currentMessages.length - 1
  streaming.value = true
  stopTypewriter()
  streamSessionId = sessionId
  abortStream = chatStream(
    { sessionId, message: text, fromMessageId, optionId: getCurrentOptionId() },
    (chunk) => {
      typewriterBuffer += chunk
      startTypewriterIfNeeded()
    },
    () => {
      streaming.value = false
      abortStream = null
      syncMessageIds(sessionId).catch(() => {})
      const isFirstMessage = conv.currentMessages.length === 2 && conv.currentMessages[1]?.role === 'assistant'
      if (!isFirstMessage) {
        fetchLatestAssistantMeta(sessionId, assistantMsgIdx).catch(() => {})
      }
      if (typewriterBuffer === '' && typewriterTimerId === null) {
        const messages = conv.currentMessages
        if (messages.length === 2 && messages[1]?.role === 'assistant' && sessionId) {
          conv.fetchConversationTitle(sessionId).then((meta) => {
            patchAssistantMeta(sessionId, assistantMsgIdx, meta?.sourceIds, meta?.toolExecutionIds)
          })
        }
      }
    },
    (err) => {
      streaming.value = false
      abortStream = null
      stopTypewriter()
      const list = conv.currentMessages
      const last = list[list.length - 1]
      if (last?.role === 'assistant') {
        conv.updateLastMessage(sessionId!, '回复出错：' + (err.message || '未知错误'))
      }
    }
  )
}

/** 重新生成：用同一条用户消息重新请求 AI 回复 */
function handleRegenerate(index: number) {
  const msg = conv.currentMessages[index]
  if (!msg || msg.role !== 'user') return
  resendFrom(index, msg.content)
}

/** 进入编辑模式 */
function handleStartEdit(index: number) {
  const msg = conv.currentMessages[index]
  if (!msg || msg.role !== 'user') return
  editingIndex.value = index
  editText.value = msg.content
}

/** 保存编辑并重新发送 */
function handleSaveEdit(index: number) {
  const text = editText.value.trim()
  if (!text) return
  editingIndex.value = null
  editText.value = ''
  resendFrom(index, text)
}

/** 取消编辑 */
function handleCancelEdit() {
  editingIndex.value = null
  editText.value = ''
}

/** 返回来源列表中去重后的知识库名称列表 */
function uniqueKbNames(sources: RagSource[]): string[] {
  const seen = new Set<string>()
  const result: string[] = []
  for (const s of sources) {
    const name = s.kbName || s.indexName || '知识库'
    if (!seen.has(name)) { seen.add(name); result.push(name) }
  }
  return result
}

function ragSourcesOf(message: { ragSources?: RagSource[] }): RagSource[] {
  return message.ragSources ?? []
}

/** 点击某个知识库 badge：展示该知识库的所有片段 */
function handleRagBadgeClick(sources: RagSource[], kbName: string) {
  ragModal.value = sources.filter((s) => (s.kbName || s.indexName || '知识库') === kbName)
}

function handleToolBadgeClick(execution: ToolExecution) {
  toolModal.value = execution
}

function autoResizeInput(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 120)}px`
}

function send() {
  const text = inputText.value.trim()
  const sessionId = conv.currentId
  if (!text || !sessionId || streaming.value) return

  conv.appendMessage(sessionId, { role: 'user', content: text })

  inputText.value = ''
  conv.appendMessage(sessionId, { role: 'assistant', content: '' })
  const assistantMsgIdx = conv.currentMessages.length - 1
  streaming.value = true
  stopTypewriter()
  streamSessionId = sessionId
  abortStream = chatStream(
    { sessionId, message: text, optionId: getCurrentOptionId() },
    (chunk) => {
      typewriterBuffer += chunk
      startTypewriterIfNeeded()
    },
    () => {
      streaming.value = false
      abortStream = null
      syncMessageIds(sessionId).catch(() => {})
      const isFirstMessage = conv.currentMessages.length === 2 && conv.currentMessages[1]?.role === 'assistant'
      if (!isFirstMessage) {
        fetchLatestAssistantMeta(sessionId, assistantMsgIdx).catch(() => {})
      }
      // 缓冲未打完时由 flushOneChar 继续打完再结束；已打完则这里直接触发标题拉取
      if (typewriterBuffer === '' && typewriterTimerId === null) {
        const messages = conv.currentMessages
        if (messages.length === 2 && messages[1]?.role === 'assistant' && sessionId) {
          conv.fetchConversationTitle(sessionId).then((meta) => {
            patchAssistantMeta(sessionId, assistantMsgIdx, meta?.sourceIds, meta?.toolExecutionIds)
          })
        }
      }
    },
    (err) => {
      streaming.value = false
      abortStream = null
      stopTypewriter()
      const list = conv.currentMessages
      const last = list[list.length - 1]
      if (last?.role === 'assistant') {
        conv.updateLastMessage(
          sessionId!,
          '回复出错：' + (err.message || '未知错误')
        )
      }
    }
  )
}
</script>

<style scoped>
.chat-page {
  height: 100vh;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

/* ── 顶栏 ───────────────────────────────────── */
.top-nav {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  height: 56px;
  padding: 0 1.25rem;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
}

.top-nav-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--color-text-accent) 10%, var(--color-bg-page));
  color: var(--color-text-accent);
}

.brand-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.nav-sep {
  color: var(--color-text-tertiary);
  font-size: 0.875rem;
}

.nav-title {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-nav-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.agent-picker {
  margin-right: 0.25rem;
}

.agent-select {
  min-width: 140px;
  max-width: 200px;
  padding: 0.375rem 2rem 0.375rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12' fill='none'%3E%3Cpath d='M2.5 4.5L6 8l3.5-3.5' stroke='%238f959e' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.625rem center;
}

.agent-select:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.nav-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.nav-icon-btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border-color: var(--color-border);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.625rem 0.25rem 0.25rem;
  border-radius: 999px;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #69b1ff, #1677ff);
}

.user-name {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-text-btn {
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.nav-text-btn:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}

/* ── 主体布局 ───────────────────────────────── */
.chat-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

/* ── 左侧会话面板 ───────────────────────────── */
.session-panel {
  width: 260px;
  min-width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.session-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--color-border);
}

.session-panel-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.new-chat-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3125rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.new-chat-btn:hover:not(:disabled) {
  background: var(--color-button-primary-hover);
}

.new-chat-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0.5rem;
  scrollbar-width: thin;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5625rem 0.75rem;
  margin-bottom: 2px;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.conversation-item:hover {
  background: var(--color-bg-page);
}

.conversation-item.active {
  background: color-mix(in srgb, var(--color-text-accent) 8%, var(--color-bg-page));
  color: var(--color-text-accent);
  font-weight: 500;
}

.conversation-delete-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}

.conversation-item:hover .conversation-delete-btn,
.conversation-item.active .conversation-delete-btn {
  opacity: 1;
}

.conversation-delete-btn:hover {
  color: #ef4444;
  background: var(--color-bg-card);
}

.conversation-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-conversations {
  padding: 1.5rem 0.75rem;
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  text-align: center;
  line-height: 1.6;
}

.conv-group-label {
  padding: 0.5rem 0.75rem 0.25rem;
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  letter-spacing: 0.04em;
}

/* ── 主对话区 ───────────────────────────────── */
.chat-stage {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.stage-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.stage-empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 0.5rem;
  border-radius: 50%;
  background: var(--color-bg-page);
  color: var(--color-text-tertiary);
}

.stage-empty-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.stage-empty-desc {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
}

.stage-empty-btn {
  margin-top: 0.75rem;
  padding: 0.5rem 1.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.stage-empty-btn:hover:not(:disabled) {
  background: var(--color-button-primary-hover);
}

.stage-empty-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.chat-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.panel-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
}

.panel-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.panel-toolbar-actions {
  display: flex;
  gap: 0.25rem;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.toolbar-btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border-color: var(--color-border);
}

.toolbar-btn.is-active {
  color: var(--color-text-accent);
  background: color-mix(in srgb, var(--color-text-accent) 8%, var(--color-bg-page));
  border-color: color-mix(in srgb, var(--color-text-accent) 30%, var(--color-border));
}

.panel-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1.25rem 1.5rem;
  scrollbar-width: thin;
}

.panel-status {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
}

/* ── 欢迎态 ─────────────────────────────────── */
.welcome-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 520px;
  margin: 2rem auto 0;
  text-align: center;
}

.welcome-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 0.75rem;
  border-radius: 50%;
  background: linear-gradient(135deg, #e6f4ff, #bae0ff);
  color: var(--color-text-accent);
}

.welcome-name {
  margin: 0 0 0.25rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.welcome-hint {
  margin: 0 0 1.25rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.welcome-bubble {
  width: 100%;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  text-align: left;
}

.welcome-bubble p {
  margin: 0;
  line-height: 1.6;
}

/* ── 消息列表 ───────────────────────────────── */
.messages {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  max-width: 720px;
  margin: 0 auto;
}

.message {
  display: flex;
  gap: 0.625rem;
}

.message.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-top: 0.125rem;
  border-radius: 50%;
  background: linear-gradient(135deg, #e6f4ff, #bae0ff);
  color: var(--color-text-accent);
}

.msg-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  max-width: 85%;
}

.message.user .msg-content {
  align-items: flex-end;
}

.bubble {
  padding: 0.625rem 0.875rem;
  border-radius: 12px;
  font-size: 0.9375rem;
  line-height: 1.65;
  word-break: break-word;
}

.message.user .bubble {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-bottom-right-radius: 4px;
  color: var(--color-text-primary);
}

.message.assistant .bubble {
  background: transparent;
  padding: 0.125rem 0;
  color: var(--color-text-primary);
}

.message-actions {
  display: flex;
  gap: 0.25rem;
  margin-top: 0.25rem;
}

.rag-badges,
.tool-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin-top: 0.25rem;
}

.rag-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  border: 1px solid var(--color-border);
  background: transparent;
  transition: color 0.15s, border-color 0.15s;
}

.rag-badge:hover {
  color: var(--color-text-accent);
  border-color: var(--color-text-accent);
}

.tool-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  font-weight: 500;
  color: #8b5cf6;
  cursor: pointer;
  padding: 0.22rem 0.62rem;
  border-radius: 999px;
  border: 1px solid rgba(139, 92, 246, 0.22);
  background: rgba(139, 92, 246, 0.08);
  transition: all 0.15s;
}

.tool-badge:hover {
  color: #7c3aed;
  border-color: rgba(124, 58, 237, 0.35);
  background: rgba(139, 92, 246, 0.14);
}

/* ── 输入区 ─────────────────────────────────── */
.panel-footer {
  flex-shrink: 0;
  padding: 0.75rem 1rem 0.625rem;
  border-top: 1px solid var(--color-border);
}

.input-box {
  display: flex;
  align-items: flex-end;
  gap: 0.375rem;
  padding: 0.5rem 0.625rem;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.input-box:focus-within {
  border-color: color-mix(in srgb, var(--color-text-accent) 50%, var(--color-border));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-text-accent) 10%, transparent);
}

.input-box.disabled {
  opacity: 0.75;
}

.input-icon-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.input-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-field {
  flex: 1;
  min-width: 0;
  min-height: 32px;
  max-height: 120px;
  padding: 0.375rem 0;
  font-size: 0.9375rem;
  font-family: inherit;
  line-height: 1.5;
  color: var(--color-text-primary);
  background: transparent;
  border: none;
  resize: none;
  outline: none;
}

.input-field::placeholder {
  color: var(--color-text-tertiary);
}

.input-send-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
}

.input-send-btn:hover:not(:disabled) {
  background: var(--color-button-primary-hover);
}

.input-send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.input-disclaimer {
  margin: 0.375rem 0 0;
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
  text-align: center;
}

/* ── 编辑区 ─────────────────────────────────── */
.edit-area {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}

.edit-textarea {
  width: 100%;
  min-height: 100px;
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-hover);
  border-radius: 12px;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
}

.edit-textarea:focus {
  border-color: var(--color-border-focus);
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.edit-btn {
  padding: 0.4rem 1rem;
  font-size: 0.8125rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.edit-btn.save {
  color: #fff;
  background: var(--color-button-primary);
  border: none;
  font-weight: 500;
}

.edit-btn.save:hover {
  background: var(--color-button-primary-hover);
}

.edit-btn.cancel {
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
}

.edit-btn.cancel:hover {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}

.action-btn {
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
  transition: all 0.15s;
}

.action-btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-page);
}

/* ── Markdown ───────────────────────────────── */
.message .content {
  font-size: 0.9375rem;
  line-height: 1.65;
  word-break: break-word;
}

.message .content.markdown-body :deep(p) { margin: 0.5em 0; }
.message .content.markdown-body :deep(p:first-child) { margin-top: 0; }
.message .content.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.message .content.markdown-body :deep(code) {
  padding: 0.2em 0.4em;
  font-size: 0.9em;
  background: var(--color-bg-input);
  border-radius: 4px;
  border: 1px solid var(--color-border);
}
.message .content.markdown-body :deep(.code-block-wrapper) { position: relative; margin: 0.5em 0; }
.message .content.markdown-body :deep(pre) {
  margin: 0;
  padding: 0.75rem 1rem;
  padding-bottom: 2rem;
  overflow-x: auto;
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
}
.message .content.markdown-body :deep(.copy-code-btn) {
  position: absolute;
  right: 0.5rem;
  bottom: 0.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--color-text-tertiary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
}
.message .content.markdown-body :deep(.copy-code-btn:hover) {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}
.message .content.markdown-body :deep(.copy-code-btn.copied) {
  color: #22c55e;
  border-color: #22c55e;
}
.message .content.markdown-body :deep(pre code) {
  padding: 0;
  background: none;
  border: none;
  font-size: 0.875rem;
}
.message .content.markdown-body :deep(ul),
.message .content.markdown-body :deep(ol) { margin: 0.5em 0; padding-left: 1.5em; }
.message .content.markdown-body :deep(li) { margin: 0.2em 0; }
.message .content.markdown-body :deep(blockquote) {
  margin: 0.5em 0;
  padding-left: 1em;
  border-left: 4px solid var(--color-border-focus);
  color: var(--color-text-secondary);
}
.message .content.markdown-body :deep(h1),
.message .content.markdown-body :deep(h2),
.message .content.markdown-body :deep(h3) {
  margin: 0.75em 0 0.35em;
  font-weight: 600;
  line-height: 1.3;
}
.message .content.markdown-body :deep(h1) { font-size: 1.25rem; }
.message .content.markdown-body :deep(h2) { font-size: 1.1rem; }
.message .content.markdown-body :deep(h3) { font-size: 1rem; }
.message .content.markdown-body :deep(a) { color: var(--color-text-accent); text-decoration: none; }
.message .content.markdown-body :deep(a:hover) { text-decoration: underline; }
.message .content.markdown-body :deep(strong) { font-weight: 600; }
.message .content.markdown-body :deep(table) { border-collapse: collapse; font-size: 0.9em; }
.message .content.markdown-body :deep(th),
.message .content.markdown-body :deep(td) { padding: 0.35em 0.6em; border: 1px solid var(--color-border); }
.message .content.markdown-body :deep(thead th) { background: var(--color-bg-input); font-weight: 600; }
.message .content.markdown-body :deep(hr) { border: none; border-top: 1px solid var(--color-border); margin: 0.75em 0; }

/* ── 弹框 ───────────────────────────────────── */
.rag-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.rag-modal {
  width: 540px;
  max-width: calc(100vw - 2rem);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.rag-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.9rem 1.1rem;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  flex-shrink: 0;
}

.rag-modal-close {
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

.rag-modal-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.rag-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 1.1rem;
}

.rag-source-item { padding: 0.75rem 0; }
.rag-source-item + .rag-source-item { border-top: 1px solid var(--color-border); }
.rag-source-kb {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-accent);
  margin-bottom: 0.35rem;
}
.rag-source-text {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

/* ── 响应式 ─────────────────────────────────── */
@media (max-width: 900px) {
  .user-name,
  .nav-sep,
  .nav-title {
    display: none;
  }

  .session-panel {
    width: 220px;
    min-width: 220px;
  }
}

@media (max-width: 680px) {
  .chat-layout {
    flex-direction: column;
    padding: 8px;
  }

  .session-panel {
    width: 100%;
    min-width: 0;
    max-height: 180px;
  }

  .user-chip {
    display: none;
  }

  .agent-select {
    min-width: 110px;
    max-width: 140px;
  }
}
</style>

