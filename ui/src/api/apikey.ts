import { $get, $post, $put, $delete } from './request'

export interface ApiKeyListItem {
  id: number
  name: string | null
  status: number
  expiresAt: string | null
  lastUsedAt: string | null
  createdAt: string | null
  apiKey: string
  chatAgentOptionId: number | null
  chatAgentOptionName: string | null
}

export interface CreateApiKeyResult {
  id: number
  apiKey: string
  name: string | null
  expiresAt: string | null
  chatAgentOptionId: number | null
  chatAgentOptionName: string | null
}

export interface CreateApiKeyBody {
  name?: string
  expiresInDays?: number
  chatAgentOptionId?: number | null
}

export function getApiKeyList() {
  return $get<ApiKeyListItem[]>('/apikey/list')
}

export function createApiKey(body?: CreateApiKeyBody) {
  return $post<CreateApiKeyResult>('/apikey/create', body ?? {})
}

export function updateApiKey(id: number, body: { chatAgentOptionId: number | null }) {
  return $put(`/apikey/${id}`, body)
}

export function disableApiKey(id: number) {
  return $post(`/apikey/disable/${id}`)
}

export function deleteApiKey(id: number) {
  return $delete(`/apikey/${id}`)
}
