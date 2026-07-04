import type { AgentModelTypeKey } from '@/api/agentOption'
import type { ModelParamFieldSchema } from '@/api/model'

/** 前端表单中的模型绑定 */
export interface ModelBindingForm {
  modelName: string
  params: Record<string, unknown>
}

/** 提交给后端的模型绑定 */
export interface ModelBindingPayload {
  modelName: string
  params?: string
}

export function emptyModelBinding(): ModelBindingForm {
  return { modelName: '', params: {} }
}

export function parseModelBinding(raw: unknown): ModelBindingForm {
  if (!raw) return emptyModelBinding()
  if (typeof raw === 'string') {
    return { modelName: raw, params: {} }
  }
  const obj = raw as { modelName?: string; params?: string | Record<string, unknown> }
  let params: Record<string, unknown> = {}
  if (typeof obj.params === 'string' && obj.params.trim()) {
    try {
      params = JSON.parse(obj.params) as Record<string, unknown>
    } catch {
      params = {}
    }
  } else if (obj.params && typeof obj.params === 'object') {
    params = { ...obj.params }
  }
  return { modelName: obj.modelName?.trim() ?? '', params }
}

export function isParamConfigured(value: unknown): boolean {
  if (value === null || value === undefined || value === '') return false
  if (Array.isArray(value) && value.length === 0) return false
  return true
}

export function countConfiguredParams(params: Record<string, unknown>): number {
  return Object.values(params).filter(isParamConfigured).length
}

export function serializeModelBinding(binding: ModelBindingForm): ModelBindingPayload | null {
  const modelName = binding.modelName?.trim()
  if (!modelName) return null
  const payload: ModelBindingPayload = { modelName }
  const configured: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(binding.params)) {
    if (isParamConfigured(value)) configured[key] = value
  }
  if (Object.keys(configured).length > 0) {
    payload.params = JSON.stringify(configured)
  }
  return payload
}

export function formatParamSummary(params: Record<string, unknown>): string {
  const entries = Object.entries(params).filter(([, v]) => isParamConfigured(v))
  if (entries.length === 0) return ''
  return entries
    .slice(0, 4)
    .map(([k, v]) => `${k}=${formatParamValue(v)}`)
    .join(' · ')
}

function formatParamValue(value: unknown): string {
  if (Array.isArray(value)) return value.join(',')
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  return String(value)
}

export function formatFieldRange(field: ModelParamFieldSchema): string | null {
  if (field.min != null && field.max != null) return `${field.min} ~ ${field.max}`
  if (field.min != null) return `≥ ${field.min}`
  if (field.max != null) return `≤ ${field.max}`
  return null
}

export function formatFieldPlaceholder(field: ModelParamFieldSchema): string {
  const parts: string[] = ['留空=默认']
  const range = formatFieldRange(field)
  if (range) parts.push(range)
  if (field.defaultValue !== null && field.defaultValue !== undefined) {
    parts.push(`默认 ${field.defaultValue}`)
  }
  return parts.join(' · ')
}

export function formatFieldHelp(field: ModelParamFieldSchema): string {
  const lines: string[] = []
  if (field.description) lines.push(`含义：${field.description}`)
  lines.push(`类型：${field.type}`)
  const range = formatFieldRange(field)
  if (range) lines.push(`范围：${range}`)
  if (field.enumValues?.length) lines.push(`可选值：${field.enumValues.join('、')}`)
  if (field.defaultValue !== null && field.defaultValue !== undefined) {
    lines.push(`模型默认值：${field.defaultValue}`)
  }
  lines.push('不填写时由模型/provider 使用内置默认值')
  if (field.required) lines.push('（必填项）')
  return lines.join('\n')
}

export function schemaCacheKey(provider: string, keyType: AgentModelTypeKey): string {
  return `${provider}:${keyType}`
}
