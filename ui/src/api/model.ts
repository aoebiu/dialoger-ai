import { $get, $post, $put, $delete } from './request'

export interface ModelApiKeyItem {
  id: number
  modelName: string
  modelProvider: string
  keyType: string
  maskedApiKey: string
  createdAt: string | null
  defaultDirectChat: boolean
}

export function getModelKeyList() {
  return $get<ModelApiKeyItem[]>('/model/list')
}

export function createModelKey(params: {
  modelName: string
  modelProvider: string
  keyType: string
  apiKey: string
}) {
  const search = new URLSearchParams()
  search.set('modelName', params.modelName)
  search.set('modelProvider', params.modelProvider)
  search.set('keyType', params.keyType)
  search.set('apiKey', params.apiKey)
  return $post<ModelApiKeyItem>(`/model/create?${search.toString()}`)
}

export function deleteModelKey(id: number) {
  return $delete(`/model/${id}`)
}

export function setDefaultDirectChatModel(id: number) {
  return $put<ModelApiKeyItem[]>(`/model/${id}/directChat`)
}

export function clearDefaultDirectChatModel(id: number) {
  return $delete<ModelApiKeyItem[]>(`/model/${id}/directChat`)
}

export type ParamValueType =
  | 'STRING'
  | 'INTEGER'
  | 'FLOAT'
  | 'DOUBLE'
  | 'BOOLEAN'
  | 'STRING_ARRAY'
  | 'ENUM'

export interface ModelParamFieldSchema {
  name: string
  type: ParamValueType
  description?: string
  required?: boolean
  defaultValue?: unknown
  min?: number | null
  max?: number | null
  enumValues?: string[] | null
}

export interface ModelParamSchemaDefinition {
  modelProvider: string
  keyType: string
  paramClass?: string
  fields: ModelParamFieldSchema[]
}

/** provider → keyType → schema */
export type ModelParamSchemasMap = Record<string, Record<string, ModelParamSchemaDefinition>>

export function getAllModelParamSchemas() {
  return $get<ModelParamSchemasMap>('/model/schema')
}

let schemaCache: ModelParamSchemasMap | null = null
let schemaLoadPromise: Promise<ModelParamSchemasMap> | null = null

/** 清除内存缓存（如登出后需重新拉取时调用） */
export function clearModelParamSchemaCache() {
  schemaCache = null
  schemaLoadPromise = null
}

/** 确保已加载完整 Schema 结构；有缓存则直接返回，否则仅发起一次请求 */
export async function ensureModelParamSchemas(): Promise<ModelParamSchemasMap> {
  if (schemaCache) return schemaCache
  if (!schemaLoadPromise) {
    schemaLoadPromise = getAllModelParamSchemas().then((res) => {
      if (!res.success || !res.data) {
        schemaLoadPromise = null
        throw new Error(res.message ?? '加载模型参数 Schema 失败')
      }
      schemaCache = res.data
      return res.data
    })
  }
  return schemaLoadPromise
}

export function lookupModelParamSchema(
  schemas: ModelParamSchemasMap,
  provider: string,
  keyType: string,
): ModelParamSchemaDefinition | undefined {
  return schemas[provider]?.[keyType]
}
