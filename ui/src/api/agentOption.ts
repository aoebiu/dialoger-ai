import { $get, $post, $put, $delete } from './request'
import type { ModelBindingForm, ModelBindingPayload } from '@/utils/modelParams'
import { emptyModelBinding } from '@/utils/modelParams'

export const AGENT_MODEL_TYPES = [
  { key: 'chat', label: '对话 (CHAT)' },
  { key: 'streaming_chat', label: '流式对话 (STREAMING_CHAT)' },
  { key: 'embedding', label: '嵌入 (EMBEDDING)' },
  { key: 'scoring', label: '评分 (SCORING)' },
  { key: 'moderation', label: '审核 (MODERATION)' },
  { key: 'image', label: '图像 (IMAGE)' },
] as const

export type AgentModelTypeKey = (typeof AGENT_MODEL_TYPES)[number]['key']

export interface BindableModelOption {
  modelName: string
  modelProvider: string
}

export interface BindableKbOption {
  id: number
  name: string
  visibility: string
}

export type BindableModelsMap = Record<AgentModelTypeKey, BindableModelOption[]>

export type AgentModelBindingsMap = Record<AgentModelTypeKey, ModelBindingForm>

export interface AgentOptionItem {
  id: number
  memberId?: number
  name: string
  maxMessages?: number | null
  enabled?: boolean | null
  rag?: boolean | null
  transform?: string | null
  contentAggregator?: boolean | null
  tools?: boolean | null
  contentInjectorPrompt?: string | null
  systemPrompt?: string | null
  inDB?: boolean | null
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  modelBindings?: Partial<Record<AgentModelTypeKey, ModelBindingPayload | string>>
  kbIds?: number[]
}

export interface AgentOptionSaveBody {
  name: string
  maxMessages?: number
  enabled?: boolean
  rag?: boolean
  transform?: string | null
  contentAggregator?: boolean
  tools?: boolean
  contentInjectorPrompt?: string | null
  systemPrompt?: string | null
  inDB?: boolean
  remark?: string | null
  modelBindings?: Partial<Record<AgentModelTypeKey, ModelBindingPayload>>
  kbIds?: number[]
}

export interface AgentOptionBindables {
  models: BindableModelsMap
  kbs: BindableKbOption[]
}

export function getAgentOptionList() {
  return $get<AgentOptionItem[]>('/agentOption/list')
}

export function getAgentOptionBindables() {
  return $get<AgentOptionBindables>('/agentOption/bindables')
}

export function createAgentOption(body: AgentOptionSaveBody) {
  return $post<AgentOptionItem>('/agentOption/create', body)
}

export function updateAgentOption(id: number, body: AgentOptionSaveBody) {
  return $put<AgentOptionItem>(`/agentOption/${id}`, body)
}

export function deleteAgentOption(id: number) {
  return $delete(`/agentOption/${id}`)
}

export function emptyModelBindings(): AgentModelBindingsMap {
  return {
    chat: emptyModelBinding(),
    streaming_chat: emptyModelBinding(),
    embedding: emptyModelBinding(),
    scoring: emptyModelBinding(),
    moderation: emptyModelBinding(),
    image: emptyModelBinding(),
  }
}

export function emptyBindableModels(): BindableModelsMap {
  return {
    chat: [],
    streaming_chat: [],
    embedding: [],
    scoring: [],
    moderation: [],
    image: [],
  }
}
