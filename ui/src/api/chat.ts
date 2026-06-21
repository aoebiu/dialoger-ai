import { $get, $post, $postStream, $delete } from './request'

export interface ChatSessionResponse {
  sessionId: string
  title: string
}

export async function createChat() {
  return $get<ChatSessionResponse>('/chat/createChat')
}

/** 会话列表项 */
export interface SessionItem {
  id: string
  sessionId?: string
  title: string
  lastModified?: string
}

/**
 * 获取当前用户所有对话会话列表（用于刷新页面时恢复侧边栏列表）
 */
export async function getSessions() {
  return $get<SessionItem[]>('/chat/sessions')
}

/** 知识库命中片段 */
export interface RagSource {
  id?: number
  kbName?: string
  indexName?: string
  text: string
}

/** 历史消息项，与 MessageItem 一致（API 可能返回大写 role：USER/ASSISTANT/SYSTEM） */
export interface HistoryMessage {
  id?: number
  role: string
  content: string
  extras?: {
    ragSources?: RagSource[]
    [key: string]: unknown
  }
}

export interface HistoryResponse {
  messages?: HistoryMessage[]
  ragSourceMap?: Record<string, number[]>
  toolExecutionMap?: Record<string, number[]>
  title?: string
}

/** 接口可能直接返回 data 为消息数组，而非 { messages } */
export type HistoryData = HistoryMessage[] | HistoryResponse

/**
 * 根据 sessionId 拉取当前对话的历史记录（用于刷新后恢复或切换会话时加载）
 */
export async function getHistory(sessionId: string) {
  return $get<HistoryData>(`/chat/history/${sessionId}`)
}

export interface ChatStreamOptions {
  sessionId: string
  message: string
  optionId?: number
  fromMessageId?: number
}

/**
 * 删除会话
 */
export async function deleteSession(sessionId: string) {
  return $delete(`/chat/sessions/${sessionId}`)
}

export interface RagSourcesData {
  sources: RagSource[]
}

/**
 * 按 rag source id 列表拉取知识库来源内容
 */
export async function getRagSources(ids: number[]) {
  if (!ids.length) return { success: true, data: { sources: [] as RagSource[] } }
  return $get<RagSourcesData>(`/chat/ragSources?ids=${ids.join(',')}`)
}

/** 工具调用记录 */
export interface ToolExecution {
  id?: number
  toolCallId?: string
  toolName: string
  arguments?: string
  result?: string
}

export interface ToolExecutionsData {
  executions: ToolExecution[]
}

/**
 * 按 tool execution id 列表拉取工具调用详情
 */
export async function getToolExecutions(ids: number[]) {
  if (!ids.length) return { success: true, data: { executions: [] as ToolExecution[] } }
  return $get<ToolExecutionsData>(`/chat/toolExecutions?ids=${ids.join(',')}`)
}

/**
 * 流式对话
 */
export function chatStream(
  options: ChatStreamOptions,
  onChunk: (text: string) => void,
  onDone?: () => void,
  onError?: (err: Error) => void
): () => void {
  const controller = new AbortController()
  $postStream(
    '/chat/stream',
    {
      sessionId: options.sessionId,
      message: options.message,
      optionId: options.optionId ?? 1,
      ...(options.fromMessageId != null ? { fromMessageId: options.fromMessageId } : {}),
    },
    { signal: controller.signal }
  )
    .then(async (res) => {
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.message || res.statusText)
      }
      const reader = res.body?.getReader()
      if (!reader) {
        onDone?.()
        return
      }
      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed) {
            // 空行对应原始流中的 \n\n，还原为段落分隔符
            onChunk('\n')
            continue
          }
          try {
            const parsed = JSON.parse(trimmed) as string
            // JSON 字符串自带换行编码，不额外添加
            if (typeof parsed === 'string') onChunk(parsed)
          } catch {
            // 原始 token：split('\n') 消耗了换行，此处补回
            onChunk(trimmed + '\n')
          }
        }
      }
      if (buffer.trim()) {
        try {
          const parsed = JSON.parse(buffer.trim()) as string
          if (typeof parsed === 'string') onChunk(parsed)
        } catch {
          onChunk(buffer.trim())
        }
      }
      onDone?.()
    })
    .catch((err) => {
      onError?.(err instanceof Error ? err : new Error(String(err)))
    })
  return () => controller.abort()
}
