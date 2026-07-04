import type { InjectionKey, ComputedRef } from 'vue'

/** 模型绑定控件 UI 场景：设置页独立编辑 vs 聊天侧栏嵌入 */
export type AgentModelUiContext = 'settings' | 'chat'

export const AGENT_MODEL_UI_CONTEXT_KEY: InjectionKey<ComputedRef<AgentModelUiContext>> = Symbol(
  'agentModelUiContext',
)
