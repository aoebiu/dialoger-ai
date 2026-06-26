import { $get, $put, $delete } from './request'

export interface BizConfigItem {
  id: number
  memberId: number
  configKey: string
  displayValue: string
  configValue?: string | null
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface BizConfigSaveBody {
  configValue: string
  remark?: string | null
}

export function getBizConfigList() {
  return $get<BizConfigItem[]>('/configs/list')
}

export function getBizConfigItem(key: string, memberId?: number) {
  const params = memberId !== undefined ? `?memberId=${memberId}` : ''
  return $get<BizConfigItem>(`/configs/${encodeURIComponent(key)}${params}`)
}

export function saveBizConfigItem(key: string, body: BizConfigSaveBody, memberId?: number) {
  const params = memberId !== undefined ? `?memberId=${memberId}` : ''
  return $put<BizConfigItem>(`/configs/${encodeURIComponent(key)}${params}`, body)
}

export function deleteBizConfigItem(key: string, memberId?: number) {
  const params = memberId !== undefined ? `?memberId=${memberId}` : ''
  return $delete(`/configs/${encodeURIComponent(key)}${params}`)
}
