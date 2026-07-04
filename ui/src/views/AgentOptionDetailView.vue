<template>
  <div class="agent-page" :class="{ 'is-embedded': embedded }">

    <!-- ── 页头 ────────────────────────────────────────────── -->
    <header v-if="!embedded" class="agent-page-header">
      <div class="agent-page-header-main">
        <button type="button" class="agent-back-btn" aria-label="返回" @click="goBack">←</button>
        <div class="agent-header-text">
          <nav class="agent-breadcrumb" aria-label="面包屑">
            <button type="button" class="agent-breadcrumb-parent" @click="goBack">Agent 配置</button>
            <span class="agent-breadcrumb-sep">/</span>
            <span class="agent-breadcrumb-current">{{ isCreate ? '新建配置' : (agentOptionForm.name || '…') }}</span>
          </nav>
        </div>
      </div>
      <div class="agent-page-header-actions">
        <!-- 查看模式：只显示配置向导 + 编辑按钮 -->
        <template v-if="!isEditing && !embedded">
          <button type="button" class="refresh-btn" @click="openWizard">
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none" style="margin-right:4px;vertical-align:-1px" aria-hidden="true">
              <path d="M6.5 1L8 4.5 12 5.5 9.5 8l.5 3.5L6.5 10 3.5 11.5 4 8 1.5 5.5 5.5 4.5z" stroke="currentColor" stroke-width="1.1" stroke-linejoin="round"/>
            </svg>
            配置向导
          </button>
          <button type="button" class="primary-action-btn" @click="startEditing">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" style="margin-right:4px;vertical-align:-1px" aria-hidden="true">
              <path d="M8.5 1.5l2 2-7 7L1 12l.5-2.5 7-7zM7.5 2.5l2 2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            编辑
          </button>
        </template>
        <!-- 编辑模式：配置向导 + 取消 + 保存 -->
        <template v-else>
          <button type="button" class="refresh-btn" @click="openWizard">
            <svg width="13" height="13" viewBox="0 0 13 13" fill="none" style="margin-right:4px;vertical-align:-1px" aria-hidden="true">
              <path d="M6.5 1L8 4.5 12 5.5 9.5 8l.5 3.5L6.5 10 3.5 11.5 4 8 1.5 5.5 5.5 4.5z" stroke="currentColor" stroke-width="1.1" stroke-linejoin="round"/>
            </svg>
            配置向导
          </button>
          <button type="button" class="refresh-btn" @click="isCreate ? goBack() : cancelEditing()">取消</button>
          <button type="button" class="primary-action-btn" :disabled="saving" @click="saveInline">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </template>
      </div>
    </header>

    <header v-else class="agent-embedded-header">
      <span class="agent-embedded-title">{{ isCreate ? '新建配置' : (agentOptionForm.name || 'Agent 配置') }}</span>
      <div class="agent-embedded-actions">
        <button type="button" class="primary-action-btn" :disabled="saving" @click="saveInline">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </header>

    <!-- ── 加载态 ──────────────────────────────────────────── -->
    <div v-if="loading" class="agent-page-loading">
      <span class="agent-spinner"/>
      <span>加载中…</span>
    </div>

    <!-- ── 主体 ──────────────────────────────────────────────── -->
    <main v-else class="agent-page-body">

      <!-- 错误提示 -->
      <div v-if="formError" class="agent-error-bar" role="alert">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true"><circle cx="7" cy="7" r="6" stroke="currentColor" stroke-width="1.4"/><path d="M7 4.5v3M7 9.5h.01" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
        {{ formError }}
      </div>

      <!-- ── 基础信息卡片 ──────────────────────────────────── -->
      <div class="agent-info-card">
        <h2 class="agent-card-title">基础信息</h2>

        <!-- 查看模式 -->
        <template v-if="!isEditing && !embedded">
          <div class="agent-view-body">
            <div class="agent-view-meta-grid">
              <div class="agent-view-meta-row">
                <span class="agent-view-meta-key">Agent 名称</span>
                <span class="agent-view-meta-val">{{ agentOptionForm.name || '—' }}</span>
              </div>
              <div class="agent-view-meta-row">
                <span class="agent-view-meta-key">消息窗口</span>
                <span class="agent-view-meta-val">
                  {{ agentOptionForm.maxMessages?.toLocaleString() ?? '128,000' }}
                  <span class="agent-view-meta-unit">tokens</span>
                </span>
              </div>
              <div class="agent-view-meta-row">
                <span class="agent-view-meta-key">备注</span>
                <span class="agent-view-meta-val" :class="{ 'is-empty': !agentOptionForm.remark }">
                  {{ agentOptionForm.remark || '—' }}
                </span>
              </div>
            </div>

            <template v-if="agentOptionForm.systemPrompt">
              <div class="agent-view-divider"/>
              <div class="agent-view-prompt-block">
                <span class="agent-view-sub-label">系统提示词</span>
                <pre class="agent-view-prompt-pre">{{ agentOptionForm.systemPrompt }}</pre>
              </div>
            </template>
          </div>

          <!-- 状态胶囊条 -->
          <div class="agent-status-strip">
            <span v-if="!embedded" class="agent-status-pill" :class="agentOptionForm.enabled ? 'pill-on' : 'pill-off'">
              <span class="agent-status-dot"/>
              {{ agentOptionForm.enabled ? '已启用' : '未启用' }}
            </span>
            <span class="agent-status-pill" :class="agentOptionForm.tools ? 'pill-on' : 'pill-off'">
              <span class="agent-status-dot"/>
              工具调用{{ agentOptionForm.tools ? '已开启' : '未开启' }}
            </span>
            <span class="agent-status-pill pill-on pill-locked">
              <span class="agent-status-dot"/>
              消息已入库
            </span>
          </div>
        </template>

        <!-- 编辑模式 -->
        <template v-else>
          <div class="agent-edit-body">
            <div class="agent-form-field">
              <label class="agent-form-label">Agent 名称 <span class="agent-required">*</span></label>
              <input
                v-model="agentOptionForm.name"
                type="text"
                class="form-input"
                placeholder="例如：默认对话助手、知识库客服"
              />
            </div>

            <div class="agent-form-row2">
              <div class="agent-form-field agent-form-field--compact">
                <label class="agent-form-label">消息窗口</label>
                <input
                  v-model.number="agentOptionForm.maxMessages"
                  type="number"
                  class="form-input agent-form-input-compact"
                  min="1"
                  placeholder="128000"
                />
                <span class="agent-form-hint">最大 token 数</span>
              </div>
              <div class="agent-form-field">
                <label class="agent-form-label">备注</label>
                <input
                  v-model="agentOptionForm.remark"
                  type="text"
                  class="form-input"
                  placeholder="可选，便于区分用途"
                />
              </div>
            </div>

            <div class="agent-form-field">
              <label class="agent-form-label">系统提示词</label>
              <textarea
                v-model="agentOptionForm.systemPrompt"
                class="form-input form-textarea agent-system-prompt"
                rows="4"
                placeholder="定义 Agent 的角色与回答风格，留空则使用内置默认提示词"
              />
            </div>

            <div class="agent-switch-grid" :class="{ 'is-compact': embedded }">
              <div
                v-if="!embedded"
                class="agent-switch-card"
                :class="{ 'is-off': !agentOptionForm.enabled, 'is-dimmed': !agentCanEnable }"
              >
                <div class="agent-switch-card-head">
                  <span class="agent-switch-card-label">启用 Agent</span>
                  <label class="agent-toggle" :class="{ 'is-disabled': !agentCanEnable }">
                    <input v-model="agentOptionForm.enabled" type="checkbox" :disabled="!agentCanEnable"/>
                    <span class="agent-toggle-track"/>
                  </label>
                </div>
                <span class="agent-switch-card-hint">{{ agentCanEnable ? '可在对话页选择此配置' : '需先绑定对话和流式模型' }}</span>
              </div>
              <div class="agent-switch-card" :class="{ 'is-off': !agentOptionForm.tools }">
                <div class="agent-switch-card-head">
                  <span class="agent-switch-card-label">工具调用</span>
                  <label class="agent-toggle">
                    <input v-model="agentOptionForm.tools" type="checkbox"/>
                    <span class="agent-toggle-track"/>
                  </label>
                </div>
                <span class="agent-switch-card-hint">允许 Function Call</span>
              </div>
              <div class="agent-switch-card is-locked is-on">
                <div class="agent-switch-card-head">
                  <span class="agent-switch-card-label">消息入库</span>
                  <label class="agent-toggle is-on is-disabled">
                    <input type="checkbox" checked disabled/>
                    <span class="agent-toggle-track"/>
                  </label>
                </div>
                <span class="agent-switch-card-hint">持久化对话历史</span>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- ── Tab 切换栏 ──────────────────────────────────── -->
      <div class="agent-tab-bar">
        <button
          type="button"
          class="agent-tab-btn"
          :class="{ 'is-active': activeTab === 'models' }"
          @click="activeTab = 'models'"
        >
          模型配置
          <span v-if="!agentCanEnable" class="agent-tab-warn" title="核心模型未配置">!</span>
        </button>
        <button
          type="button"
          class="agent-tab-btn"
          :class="{ 'is-active': activeTab === 'rag' }"
          @click="activeTab = 'rag'"
        >
          知识库
        </button>
      </div>

      <!-- ── Tab 面板：模型配置 ────────────────────────────── -->
      <div v-show="activeTab === 'models'" class="agent-tab-panel">

        <!-- 核心模型 -->
        <div class="agent-model-sub">
          <div class="agent-model-sub-head">
            <span class="agent-model-sub-title">核心模型</span>
            <span class="agent-badge agent-badge--required">必填</span>
            <span class="agent-model-sub-desc">驱动对话的主模型，两者均需配置</span>
          </div>
          <!-- 查看模式 -->
          <div v-if="!isEditing && !embedded" class="agent-view-model-grid">
            <AgentModelBindingField
              v-for="modelType in AGENT_CORE_MODEL_TYPES"
              :key="modelType.key"
              view-mode
              :model-type-key="modelType.key"
              :label="modelType.label"
              :binding="agentOptionForm.modelBindings[modelType.key]"
              :models="bindableModels[modelType.key] ?? []"
            />
          </div>
          <!-- 编辑模式 -->
          <div v-else class="agent-model-grid">
            <AgentModelBindingField
              v-for="modelType in AGENT_CORE_MODEL_TYPES"
              :key="modelType.key"
              :model-type-key="modelType.key"
              :label="modelType.label"
              :binding="agentOptionForm.modelBindings[modelType.key]"
              :models="bindableModels[modelType.key] ?? []"
            />
          </div>
        </div>

        <div class="agent-model-divider"/>

        <!-- 知识库模型 -->
        <div class="agent-model-sub">
          <div class="agent-model-sub-head">
            <span class="agent-model-sub-title">知识库模型</span>
            <span class="agent-badge agent-badge--optional">可选</span>
            <span class="agent-model-sub-desc">嵌入模型是 RAG 的前提；评分模型用于结果重排序</span>
          </div>
          <!-- 查看模式 -->
          <div v-if="!isEditing && !embedded" class="agent-view-model-grid">
            <AgentModelBindingField
              view-mode
              model-type-key="embedding"
              label="嵌入 (EMBEDDING)"
              :binding="agentOptionForm.modelBindings.embedding"
              :models="bindableModels.embedding ?? []"
            />
            <AgentModelBindingField
              view-mode
              model-type-key="scoring"
              label="评分 (SCORING)"
              :binding="agentOptionForm.modelBindings.scoring"
              :models="bindableModels.scoring ?? []"
            />
          </div>
          <!-- 编辑模式 -->
          <div v-else class="agent-model-grid">
            <AgentModelBindingField
              model-type-key="embedding"
              label="嵌入 (EMBEDDING)"
              optional
              empty-option-label="不配置"
              :binding="agentOptionForm.modelBindings.embedding"
              :models="bindableModels.embedding ?? []"
            />
            <AgentModelBindingField
              model-type-key="scoring"
              label="评分 (SCORING)"
              optional
              empty-option-label="不配置"
              :binding="agentOptionForm.modelBindings.scoring"
              :models="bindableModels.scoring ?? []"
              :disabled="!agentCanRag"
            >
              <template #label-note>
                <span v-if="!agentCanRag" class="agent-form-label-note">需先配置嵌入模型</span>
              </template>
            </AgentModelBindingField>
          </div>
        </div>

        <div class="agent-model-divider"/>

        <!-- 扩展模型 -->
        <div class="agent-model-sub">
          <div class="agent-model-sub-head">
            <span class="agent-model-sub-title">扩展模型</span>
            <span class="agent-badge agent-badge--optional">可选</span>
            <span class="agent-model-sub-desc">按需绑定审核、图像等辅助模型</span>
          </div>
          <!-- 查看模式 -->
          <div v-if="!isEditing && !embedded" class="agent-view-model-grid">
            <AgentModelBindingField
              v-for="modelType in AGENT_OTHER_MODEL_TYPES"
              :key="modelType.key"
              view-mode
              :model-type-key="modelType.key"
              :label="modelType.label"
              :binding="agentOptionForm.modelBindings[modelType.key]"
              :models="bindableModels[modelType.key] ?? []"
            />
          </div>
          <!-- 编辑模式 -->
          <div v-else class="agent-model-grid">
            <AgentModelBindingField
              v-for="modelType in AGENT_OTHER_MODEL_TYPES"
              :key="modelType.key"
              :model-type-key="modelType.key"
              :label="modelType.label"
              optional
              :binding="agentOptionForm.modelBindings[modelType.key]"
              :models="bindableModels[modelType.key] ?? []"
            />
          </div>
        </div>
      </div>

      <!-- ── Tab 面板：知识库 & RAG ────────────────────────── -->
      <div v-show="activeTab === 'rag'" class="agent-tab-panel">

        <!-- 未配置 embedding 时的引导 -->
        <div v-if="!agentCanRag" class="agent-rag-guide">
          <div class="agent-rag-guide-icon" aria-hidden="true">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <path d="M14 4C8.477 4 4 8.477 4 14s4.477 10 10 10 10-4.477 10-10S19.523 4 14 4z" stroke="currentColor" stroke-width="1.4" stroke-dasharray="3.5 2.5"/>
              <path d="M14 10.5v5M14 17.5h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="agent-rag-guide-text">
            <p class="agent-rag-guide-title">尚未配置嵌入模型</p>
            <p class="agent-rag-guide-desc">RAG 功能需要嵌入模型支持。请先切换到 <strong>模型配置</strong> 标签，在"知识库模型"中选择嵌入模型后再返回此处配置。</p>
            <button type="button" class="refresh-btn" style="margin-top:0.75rem" @click="activeTab = 'models'">前往模型配置 →</button>
          </div>
        </div>

        <template v-else>
          <!-- 查看模式 -->
          <template v-if="!isEditing && !embedded">
            <div class="agent-rag-row">
              <div class="agent-rag-row-text">
                <span class="agent-rag-row-label">RAG 检索</span>
                <span class="agent-rag-row-hint">从绑定的知识库中召回相关内容并注入对话上下文</span>
              </div>
              <span class="agent-status-pill" :class="agentOptionForm.rag ? 'pill-on' : 'pill-off'">
                <span class="agent-status-dot"/>
                {{ agentOptionForm.rag ? '已启用' : '未启用' }}
              </span>
            </div>

            <template v-if="agentOptionForm.rag">
              <div class="agent-model-divider"/>
              <div class="agent-kb-section">
                <div class="agent-kb-section-head">
                  <span class="agent-form-label">已绑定知识库</span>
                  <span class="agent-kb-count">{{ agentOptionForm.kbIds.length }} 个</span>
                </div>
                <div v-if="agentOptionForm.kbIds.length > 0" class="agent-view-kb-tags">
                  <span
                    v-for="kbId in agentOptionForm.kbIds"
                    :key="kbId"
                    class="agent-view-kb-tag"
                  >{{ getKbName(kbId) }}</span>
                </div>
                <p v-else class="agent-kb-empty">未绑定任何知识库</p>
              </div>

              <template v-if="agentCanContentAggregator">
                <div class="agent-model-divider"/>
                <div class="agent-rag-row">
                  <div class="agent-rag-row-text">
                    <span class="agent-rag-row-label">内容重排序</span>
                    <span class="agent-rag-row-hint">使用评分模型对 RAG 召回结果进行二次排序，提升相关性</span>
                  </div>
                  <span class="agent-status-pill" :class="agentOptionForm.contentAggregator ? 'pill-on' : 'pill-off'">
                    <span class="agent-status-dot"/>
                    {{ agentOptionForm.contentAggregator ? '已启用' : '未启用' }}
                  </span>
                </div>
              </template>
            </template>
          </template>

          <!-- 编辑模式 -->
          <template v-else>
            <div class="agent-rag-row">
              <div class="agent-rag-row-text">
                <span class="agent-rag-row-label">启用 RAG 检索</span>
                <span class="agent-rag-row-hint">从绑定的知识库中召回相关内容并注入对话上下文</span>
              </div>
              <label class="agent-toggle">
                <input v-model="agentOptionForm.rag" type="checkbox"/>
                <span class="agent-toggle-track"/>
              </label>
            </div>

            <template v-if="agentOptionForm.rag">
              <div class="agent-model-divider"/>
              <div class="agent-kb-section">
                <div class="agent-kb-section-head">
                  <span class="agent-form-label">知识库绑定</span>
                  <span v-if="bindableKnowledgeBases.length > 0" class="agent-kb-count">
                    已选 {{ agentOptionForm.kbIds.length }} / {{ bindableKnowledgeBases.length }}
                  </span>
                </div>
                <div v-if="bindableKnowledgeBases.length > 0" class="agent-kb-list">
                  <label
                    v-for="kb in bindableKnowledgeBases"
                    :key="kb.id"
                    class="agent-kb-item"
                    :class="{ 'is-selected': agentOptionForm.kbIds.includes(kb.id) }"
                  >
                    <input v-model="agentOptionForm.kbIds" type="checkbox" class="agent-kb-item-input" :value="kb.id"/>
                    <span class="agent-kb-item-check" aria-hidden="true"/>
                    <span class="agent-kb-item-name">{{ kb.name }}</span>
                    <span class="agent-kb-item-tag">{{ kb.visibility === 'public' ? '公开' : '私有' }}</span>
                  </label>
                </div>
                <p v-else class="agent-kb-empty">暂无可绑定的知识库，请先在文档知识库中创建并激活</p>
              </div>

              <template v-if="agentCanContentAggregator">
                <div class="agent-model-divider"/>
                <div class="agent-rag-row">
                  <div class="agent-rag-row-text">
                    <span class="agent-rag-row-label">内容重排序</span>
                    <span class="agent-rag-row-hint">使用评分模型对 RAG 召回结果进行二次排序，提升相关性</span>
                  </div>
                  <label class="agent-toggle">
                    <input v-model="agentOptionForm.contentAggregator" type="checkbox"/>
                    <span class="agent-toggle-track"/>
                  </label>
                </div>
              </template>
            </template>
          </template>
        </template>
      </div>

    </main>

    <!-- ── 配置向导弹框 ──────────────────────────────────────── -->
    <div v-if="showWizard" class="modal-overlay agent-modal-overlay">
      <div class="modal-content modal-agent-option">
        <div class="modal-header agent-modal-header">
          <div class="agent-modal-header-main">
            <div class="agent-modal-title-row">
              <h2 class="modal-title">{{ isCreate ? '添加 Agent 配置' : '编辑 Agent 配置' }}</h2>
              <span class="agent-step-badge">步骤 {{ wizardStep + 1 }} / {{ AGENT_WIZARD_STEPS.length }}</span>
            </div>
            <div class="agent-progress-track" aria-hidden="true">
              <div class="agent-progress-fill" :style="{ width: `${((wizardStep + 1) / AGENT_WIZARD_STEPS.length) * 100}%` }"/>
            </div>
          </div>
          <button type="button" class="modal-close agent-modal-close" aria-label="关闭" @click="closeWizard">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
              <path d="M4.5 4.5l9 9M13.5 4.5l-9 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="agent-wizard-layout">
          <nav class="agent-wizard-sidebar" aria-label="配置步骤">
            <button
              v-for="(step, idx) in AGENT_WIZARD_STEPS"
              :key="step.key"
              type="button"
              class="agent-wizard-sidebar-item"
              :class="{ 'is-active': idx === wizardStep, 'is-done': idx < wizardStep }"
              :aria-current="idx === wizardStep ? 'step' : undefined"
              @click="goToStep(idx)"
            >
              <span class="agent-wizard-sidebar-indicator">
                <span v-if="idx < wizardStep" class="agent-wizard-check" aria-hidden="true">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2 6l3 3 5-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </span>
                <span v-else class="agent-wizard-sidebar-num">{{ idx + 1 }}</span>
              </span>
              <span class="agent-wizard-sidebar-text">
                <span class="agent-wizard-sidebar-label">{{ step.label }}</span>
                <span class="agent-wizard-sidebar-desc">{{ step.desc }}</span>
              </span>
            </button>
          </nav>

          <div class="modal-body agent-option-modal-body">
            <Transition name="agent-slide" mode="out-in">
              <div :key="wizardStep" class="agent-step-panel">
                <header class="agent-step-header">
                  <h3 class="agent-step-title">{{ (AGENT_WIZARD_STEPS[wizardStep] ?? AGENT_WIZARD_STEPS[0]).label }}</h3>
                  <p class="agent-step-desc">{{ (AGENT_WIZARD_STEPS[wizardStep] ?? AGENT_WIZARD_STEPS[0]).desc }}</p>
                </header>
                <div class="agent-step-card">
                  <!-- Step 0: 基础信息 -->
                  <template v-if="wizardStep === 0">
                    <div class="agent-step-fields">
                      <div class="agent-field agent-field-stack">
                        <label class="agent-field-label">Agent 名称 <span class="required">*</span></label>
                        <input v-model="agentOptionForm.name" type="text" class="form-input" placeholder="例如：默认对话、知识库助手"/>
                      </div>
                      <div class="agent-field agent-field-stack">
                        <label class="agent-field-label">消息窗口</label>
                        <input v-model.number="agentOptionForm.maxMessages" type="number" class="form-input agent-field-input-narrow" min="1" placeholder="128000"/>
                      </div>
                      <div class="agent-field agent-field-stack">
                        <label class="agent-field-label">备注</label>
                        <input v-model="agentOptionForm.remark" type="text" class="form-input" placeholder="可选，便于区分不同 Agent 用途"/>
                      </div>
                    </div>
                  </template>
                  <!-- Step 1: 对话模型 -->
                  <template v-else-if="wizardStep === 1">
                    <p class="agent-step-note">绑定对话与流式对话模型，两者均为必填</p>
                    <div class="agent-model-grid">
                      <AgentModelBindingField
                        v-for="modelType in AGENT_CORE_MODEL_TYPES"
                        :key="modelType.key"
                        field-class="agent-field agent-field-stack"
                        :model-type-key="modelType.key"
                        :label="modelType.label"
                        :binding="agentOptionForm.modelBindings[modelType.key]"
                        :models="bindableModels[modelType.key] ?? []"
                      />
                    </div>
                  </template>
                  <!-- Step 2: 运行设置 -->
                  <template v-else-if="wizardStep === 2">
                    <div class="agent-switch-group">
                      <div class="agent-field agent-field-switch">
                        <div class="agent-field-meta"><span class="agent-field-label">启用 Agent</span><span class="agent-field-hint">开启后可在对话页选择使用</span></div>
                        <label class="agent-switch"><input v-model="agentOptionForm.enabled" type="checkbox"/><span class="agent-switch-track"/></label>
                      </div>
                      <div class="agent-field agent-field-switch">
                        <div class="agent-field-meta"><span class="agent-field-label">工具调用</span><span class="agent-field-hint">允许 Function Call</span></div>
                        <label class="agent-switch"><input v-model="agentOptionForm.tools" type="checkbox"/><span class="agent-switch-track"/></label>
                      </div>
                      <div class="agent-field agent-field-switch is-locked">
                        <div class="agent-field-meta"><span class="agent-field-label">消息入库</span><span class="agent-field-hint">持久化对话历史</span></div>
                        <label class="agent-switch is-on is-disabled"><input type="checkbox" checked disabled/><span class="agent-switch-track"/></label>
                      </div>
                    </div>
                    <div class="agent-step-fields agent-step-fields--compact">
                      <div class="agent-field agent-field-stack">
                        <label class="agent-field-label">系统提示词</label>
                        <textarea v-model="agentOptionForm.systemPrompt" class="form-input form-textarea agent-prompt-textarea" rows="5" placeholder="定义 Agent 的角色与回答风格，留空使用内置默认"/>
                      </div>
                    </div>
                  </template>
                  <!-- Step 3: RAG -->
                  <template v-else-if="wizardStep === 3">
                    <p class="agent-step-note">可选配置，不启用 RAG 可直接点击下一步跳过</p>
                    <div class="agent-step-fields">
                      <AgentModelBindingField
                        field-class="agent-field agent-field-stack"
                        model-type-key="embedding"
                        label="嵌入 (EMBEDDING)"
                        optional
                        empty-option-label="不配置（跳过 RAG）"
                        :binding="agentOptionForm.modelBindings.embedding"
                        :models="bindableModels.embedding ?? []"
                      />
                      <template v-if="agentCanRag">
                        <div class="agent-rag-section">
                          <div class="agent-field agent-field-switch agent-field-switch--flat">
                            <div class="agent-field-meta"><span class="agent-field-label">启用 RAG 检索</span><span class="agent-field-hint">从绑定的知识库召回相关内容</span></div>
                            <label class="agent-switch"><input v-model="agentOptionForm.rag" type="checkbox"/><span class="agent-switch-track"/></label>
                          </div>
                          <div class="agent-field agent-field-stack">
                            <div class="agent-field-block-head">
                              <label class="agent-field-label">知识库绑定</label>
                              <span v-if="bindableKnowledgeBases.length > 0" class="agent-kb-count">已选 {{ agentOptionForm.kbIds.length }} / {{ bindableKnowledgeBases.length }}</span>
                            </div>
                            <div v-if="bindableKnowledgeBases.length > 0" class="agent-kb-list">
                              <label v-for="kb in bindableKnowledgeBases" :key="kb.id" class="agent-kb-item" :class="{ 'is-selected': agentOptionForm.kbIds.includes(kb.id) }">
                                <input v-model="agentOptionForm.kbIds" type="checkbox" class="agent-kb-item-input" :value="kb.id"/>
                                <span class="agent-kb-item-check"/><span class="agent-kb-item-name">{{ kb.name }}</span><span class="agent-kb-item-tag">{{ kb.visibility === 'public' ? '公开' : '私有' }}</span>
                              </label>
                            </div>
                            <p v-else class="agent-kb-empty">暂无可绑定的知识库，请先在文档知识库中创建并激活</p>
                          </div>
                          <template v-if="agentOptionForm.rag">
                            <AgentModelBindingField
                              field-class="agent-field agent-field-stack"
                              model-type-key="scoring"
                              label="评分 (SCORING)"
                              optional
                              :binding="agentOptionForm.modelBindings.scoring"
                              :models="bindableModels.scoring ?? []"
                            />
                            <div v-if="agentCanContentAggregator" class="agent-field agent-field-switch agent-field-switch--flat">
                              <div class="agent-field-meta"><span class="agent-field-label">内容重排序</span><span class="agent-field-hint">对 RAG 召回结果二次排序</span></div>
                              <label class="agent-switch"><input v-model="agentOptionForm.contentAggregator" type="checkbox"/><span class="agent-switch-track"/></label>
                            </div>
                          </template>
                        </div>
                      </template>
                    </div>
                  </template>
                  <!-- Step 4: 扩展模型 -->
                  <template v-else-if="wizardStep === 4">
                    <p class="agent-step-note">按需绑定审核、图像等辅助模型，均可跳过</p>
                    <div class="agent-model-grid">
                      <AgentModelBindingField
                        v-for="modelType in AGENT_OTHER_MODEL_TYPES"
                        :key="modelType.key"
                        field-class="agent-field agent-field-stack"
                        :model-type-key="modelType.key"
                        :label="modelType.label"
                        optional
                        :binding="agentOptionForm.modelBindings[modelType.key]"
                        :models="bindableModels[modelType.key] ?? []"
                      />
                    </div>
                  </template>
                </div>
              </div>
            </Transition>
            <p v-if="wizardError" class="form-error agent-form-error">{{ wizardError }}</p>
          </div>
        </div>

        <div class="modal-footer agent-option-modal-footer agent-wizard-footer">
          <button v-if="wizardStep > 0" type="button" class="btn-cancel agent-wizard-prev" @click="wizardPrevStep">上一步</button>
          <span v-else class="agent-wizard-prev-placeholder"/>
          <div class="agent-wizard-footer-actions">
            <button type="button" class="btn-cancel" @click="closeWizard">取消</button>
            <button v-if="wizardStep < AGENT_WIZARD_STEPS.length - 1" type="button" class="btn-cancel" @click="wizardNextStep">下一步</button>
            <button
              v-if="!isCreate || wizardStep === AGENT_WIZARD_STEPS.length - 1"
              type="button"
              class="btn-confirm"
              :disabled="saving"
              @click="saveFromWizard"
            >{{ saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AGENT_MODEL_TYPES,
  getAgentOptionList,
  getAgentOptionBindables,
  createAgentOption,
  updateAgentOption,
  emptyModelBindings,
  emptyBindableModels,
} from '@/api/agentOption'
import type {
  AgentOptionItem,
  AgentOptionSaveBody,
  BindableModelsMap,
  BindableKbOption,
  AgentModelTypeKey,
} from '@/api/agentOption'
import AgentModelBindingField from '@/components/AgentModelBindingField.vue'
import { parseModelBinding, serializeModelBinding } from '@/utils/modelParams'
import { AGENT_MODEL_UI_CONTEXT_KEY } from '@/constants/agentModelUi'
import { toastError, toastSuccess } from '@/utils/toast'

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    optionId?: number | null
    creating?: boolean
    /** 嵌入模式下由父组件传入，避免重复请求 list */
    initialItem?: AgentOptionItem | null
  }>(),
  {
    embedded: false,
    optionId: null,
    creating: false,
    initialItem: null,
  },
)

const emit = defineEmits<{
  saved: [item: AgentOptionItem]
  close: []
}>()

provide(
  AGENT_MODEL_UI_CONTEXT_KEY,
  computed(() => (props.embedded ? 'chat' : 'settings')),
)

const route = useRoute()
const router = useRouter()

const AGENT_CORE_MODEL_TYPES = AGENT_MODEL_TYPES.filter(
  (t) => t.key === 'chat' || t.key === 'streaming_chat',
)
const AGENT_OTHER_MODEL_TYPES = AGENT_MODEL_TYPES.filter(
  (t) => t.key === 'moderation' || t.key === 'image',
)
const AGENT_WIZARD_STEPS = [
  { key: 'basic',   label: '基础信息', desc: '名称、消息窗口与备注' },
  { key: 'models',  label: '对话模型', desc: '绑定对话与流式对话模型' },
  { key: 'runtime', label: '运行设置', desc: '开关与系统提示词' },
  { key: 'rag',     label: '知识库设置', desc: '检索增强与知识库绑定' },
  { key: 'extra',   label: '扩展模型', desc: '审核、图像等辅助模型' },
] as const

// ── route ─────────────────────────────────────────────────────
const isCreate = computed(() => (props.embedded ? props.creating : route.name === 'agentOptionCreate'))
const editId = computed(() => {
  if (props.embedded) return props.creating ? null : props.optionId
  const id = route.params.id
  return id ? Number(id) : null
})

// ── state ─────────────────────────────────────────────────────
const loading = ref(false)
const saving  = ref(false)
const formError = ref('')
const editingItem = ref<AgentOptionItem | null>(null)
const bindableModels = ref<BindableModelsMap>(emptyBindableModels())
const bindableKnowledgeBases = ref<BindableKbOption[]>([])

const activeTab = ref<'models' | 'rag'>('models')

// ── edit mode ─────────────────────────────────────────────────
const isEditing = ref(false)
const savedForm = ref<null | string>(null)

function startEditing() {
  savedForm.value = JSON.stringify(agentOptionForm.value)
  isEditing.value = true
}

function cancelEditing() {
  if (savedForm.value) {
    agentOptionForm.value = JSON.parse(savedForm.value)
    savedForm.value = null
  }
  formError.value = ''
  if (!props.embedded) {
    isEditing.value = false
  }
}

// ── form ──────────────────────────────────────────────────────
const agentOptionForm = ref({
  name: '',
  maxMessages: 128000,
  enabled: false,
  rag: false,
  tools: false,
  contentAggregator: false,
  systemPrompt: '',
  remark: '',
  modelBindings: emptyModelBindings(),
  kbIds: [] as number[],
})

// ── display helpers ───────────────────────────────────────────
function getKbName(id: number): string {
  const kb = bindableKnowledgeBases.value.find((k) => k.id === id)
  return kb ? kb.name : `#${id}`
}

// ── computed ──────────────────────────────────────────────────
function hasAgentModelBinding(key: AgentModelTypeKey): boolean {
  return Boolean(agentOptionForm.value.modelBindings[key]?.modelName?.trim())
}

const agentCanEnable = computed(
  () => hasAgentModelBinding('chat') && hasAgentModelBinding('streaming_chat'),
)
const agentCanRag = computed(() => hasAgentModelBinding('embedding'))
const agentCanContentAggregator = computed(() => hasAgentModelBinding('scoring'))

// ── watches ───────────────────────────────────────────────────
watch(
  () => [
    agentOptionForm.value.modelBindings.chat.modelName,
    agentOptionForm.value.modelBindings.streaming_chat.modelName,
  ],
  () => {
    if (!agentCanEnable.value) {
      if (!props.embedded) agentOptionForm.value.enabled = false
      return
    }
    if (props.embedded) agentOptionForm.value.enabled = true
  },
)
watch(
  () => agentOptionForm.value.modelBindings.embedding.modelName,
  () => {
    if (!agentCanRag.value) {
      agentOptionForm.value.rag = false
      agentOptionForm.value.kbIds = []
    }
  },
)
watch(
  () => agentOptionForm.value.rag,
  (rag) => {
    if (!rag) {
      Object.assign(agentOptionForm.value.modelBindings.scoring, { modelName: '', params: {} })
      agentOptionForm.value.contentAggregator = false
    }
  },
)
watch(
  () => agentOptionForm.value.modelBindings.scoring.modelName,
  () => { if (!agentCanContentAggregator.value) agentOptionForm.value.contentAggregator = false },
)

// ── wizard ────────────────────────────────────────────────────
const showWizard = ref(false)
const wizardStep = ref(0)
const wizardError = ref('')

function openWizard() {
  if (!isEditing.value && !isCreate.value) startEditing()
  wizardError.value = ''
  wizardStep.value = 0
  showWizard.value = true
}
function closeWizard() {
  showWizard.value = false
  wizardError.value = ''
  wizardStep.value = 0
}
function goToStep(idx: number) {
  wizardError.value = ''
  wizardStep.value = idx
}

function validateStep(stepIndex: number): string | null {
  const form = agentOptionForm.value
  switch (stepIndex) {
    case 0: return form.name.trim() ? null : '请输入 Agent 名称'
    case 1:
      if (!hasAgentModelBinding('chat'))            return '请选择对话模型'
      if (!hasAgentModelBinding('streaming_chat'))  return '请选择流式对话模型'
      return null
    default: return null
  }
}

function wizardNextStep() {
  wizardError.value = ''
  const err = validateStep(wizardStep.value)
  if (err) { wizardError.value = err; return }
  if (wizardStep.value < AGENT_WIZARD_STEPS.length - 1) wizardStep.value++
}
function wizardPrevStep() {
  wizardError.value = ''
  if (wizardStep.value > 0) wizardStep.value--
}

// ── data ──────────────────────────────────────────────────────
async function loadBindables() {
  try {
    const res = await getAgentOptionBindables()
    if (res.success && res.data) {
      bindableModels.value = { ...emptyBindableModels(), ...res.data.models }
      bindableKnowledgeBases.value = res.data.kbs ?? []
    }
  } catch { /* ignore */ }
}

function applyItemToForm(item: AgentOptionItem) {
  const bindings = emptyModelBindings()
  if (item.modelBindings) {
    for (const type of AGENT_MODEL_TYPES) {
      bindings[type.key] = parseModelBinding(item.modelBindings[type.key])
    }
  }
  agentOptionForm.value = {
    name: item.name,
    maxMessages: item.maxMessages ?? 128000,
    enabled: props.embedded ? true : (item.enabled ?? false),
    rag: item.rag ?? false,
    tools: item.tools ?? false,
    contentAggregator: item.contentAggregator ?? false,
    systemPrompt: item.systemPrompt ?? '',
    remark: item.remark ?? '',
    modelBindings: bindings,
    kbIds: [...(item.kbIds ?? [])],
  }
}

function applyEmbeddedItem(item: AgentOptionItem) {
  editingItem.value = item
  applyItemToForm(item)
  isEditing.value = true
}

async function loadEditData() {
  if (isCreate.value || editId.value === null) return
  if (props.embedded && props.initialItem?.id === editId.value) {
    applyEmbeddedItem(props.initialItem)
    return
  }
  loading.value = true
  try {
    const res = await getAgentOptionList()
    if (res.success && Array.isArray(res.data)) {
      const item = res.data.find((a) => a.id === editId.value)
      if (item) {
        editingItem.value = item
        applyItemToForm(item)
        if (props.embedded) isEditing.value = true
      }
    }
  } catch (e: unknown) {
    toastError(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function resetCreateForm() {
  editingItem.value = null
  agentOptionForm.value = {
    name: '',
    maxMessages: 128000,
    enabled: props.embedded,
    rag: false,
    tools: false,
    contentAggregator: false,
    systemPrompt: '',
    remark: '',
    modelBindings: emptyModelBindings(),
    kbIds: [],
  }
  formError.value = ''
  isEditing.value = true
}

// ── save ──────────────────────────────────────────────────────
function buildPayload(): AgentOptionSaveBody {
  const form = agentOptionForm.value
  const modelBindings: AgentOptionSaveBody['modelBindings'] = {}
  for (const type of AGENT_MODEL_TYPES) {
    const serialized = serializeModelBinding(form.modelBindings[type.key])
    if (serialized) modelBindings[type.key] = serialized
  }
  return {
    name: form.name.trim(),
    maxMessages: form.maxMessages,
    enabled: props.embedded ? (agentCanEnable.value ? true : false) : form.enabled,
    rag: form.rag,
    tools: form.tools,
    contentAggregator: form.contentAggregator,
    inDB: true,
    transform: null,
    contentInjectorPrompt: null,
    systemPrompt: form.systemPrompt.trim() || null,
    remark: form.remark.trim() || null,
    modelBindings,
    kbIds: [...form.kbIds],
  }
}

function validateAll(): string | null {
  for (let i = 0; i < AGENT_WIZARD_STEPS.length; i++) {
    const err = validateStep(i)
    if (err) return err
  }
  if (agentOptionForm.value.enabled && !agentCanEnable.value)
    return '启用 Agent 需先配置对话模型与流式对话模型'
  return null
}

async function doSave(): Promise<boolean> {
  const err = validateAll()
  if (err) { formError.value = err; wizardError.value = err; return false }
  saving.value = true
  try {
    const payload = buildPayload()
    const res = editingItem.value
      ? await updateAgentOption(editingItem.value.id, payload)
      : await createAgentOption(payload)
    if (res.success) {
      toastSuccess('保存成功')
      if (res.data) {
        editingItem.value = res.data
        applyItemToForm(res.data)
      }
      if (props.embedded) {
        if (res.data) emit('saved', res.data)
        return true
      }
      if (isCreate.value && res.data?.id) {
        router.replace({ name: 'agentOptionDetail', params: { id: res.data.id } })
      }
      return true
    }
    const msg = res.message || '保存失败'
    formError.value = msg
    wizardError.value = msg
    return false
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    formError.value = msg
    wizardError.value = msg
    return false
  } finally {
    saving.value = false
  }
}

async function saveInline() {
  formError.value = ''
  const ok = await doSave()
  if (ok && !isCreate.value && !props.embedded) {
    isEditing.value = false
    savedForm.value = null
  }
}

async function saveFromWizard() {
  wizardError.value = ''
  const ok = await doSave()
  if (ok) {
    closeWizard()
    if (!isCreate.value && !props.embedded) {
      isEditing.value = false
      savedForm.value = null
    }
  }
}

// ── navigation ────────────────────────────────────────────────
function goBack() {
  if (props.embedded) {
    emit('close')
    return
  }
  router.push({ name: 'settings', query: { section: 'agentOption' } })
}

watch(
  () => [props.embedded, props.optionId, props.creating, props.initialItem?.id] as const,
  ([embedded, optionId, creating]) => {
    if (!embedded) return
    formError.value = ''
    if (creating) {
      resetCreateForm()
      return
    }
    if (optionId != null) {
      loadEditData()
    }
  },
  { immediate: true },
)

// ── init ──────────────────────────────────────────────────────
onMounted(() => {
  loadBindables()
  if (props.embedded) return
  if (isCreate.value) {
    isEditing.value = true
  } else {
    loadEditData()
  }
})
</script>

<style scoped>
/* ══════════════════════════════════════════════
   共享表单、按钮与弹窗（独立路由页需自带，不依赖 SettingsView）
══════════════════════════════════════════════ */
.primary-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-button-primary, var(--color-text-accent));
  border: 1px solid var(--color-button-primary, var(--color-text-accent));
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s ease, opacity 0.2s ease, border-color 0.2s ease;
}
.primary-action-btn:hover:not(:disabled) {
  background: var(--color-button-primary-hover, var(--color-text-accent));
  opacity: 0.92;
}
.primary-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}
.refresh-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-border-focus, var(--color-border-hover));
  background: var(--color-bg-input);
}
.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-input,
.form-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0.625rem 0.875rem;
  font-size: 0.9375rem;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover, var(--color-border));
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.form-input:focus,
.form-textarea:focus {
  border-color: var(--color-border-focus, var(--color-text-accent));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-text-accent) 12%, transparent);
}
.form-input:disabled,
.form-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
select.form-input {
  appearance: none;
  padding-right: 2.25rem;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12' fill='none'%3E%3Cpath d='M2.5 4.5L6 8l3.5-3.5' stroke='%2394a3b8' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  cursor: pointer;
}
.form-textarea {
  resize: vertical;
  min-height: 2.5rem;
  line-height: 1.5;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
}
.modal-content {
  display: flex;
  flex-direction: column;
  width: 90%;
  max-width: 420px;
  max-height: 85vh;
  background: var(--color-bg-card);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}
.modal-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-primary);
}
.modal-close {
  padding: 0.25rem;
  font-size: 0;
  line-height: 1;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}
.modal-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}
.modal-body {
  flex: 1;
  min-height: 0;
  padding: 1.25rem;
  overflow-y: auto;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-shrink: 0;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
}
.btn-cancel,
.btn-confirm {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-cancel {
  color: var(--color-text-primary);
  background: transparent;
  border: 1px solid var(--color-border-hover, var(--color-border));
}
.btn-cancel:hover {
  border-color: var(--color-border-focus, var(--color-text-accent));
}
.btn-confirm {
  color: #fff;
  background: var(--color-text-accent);
  border: 1px solid var(--color-text-accent);
}
.btn-confirm:hover:not(:disabled) {
  opacity: 0.9;
}
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ══════════════════════════════════════════════
   页面框架
══════════════════════════════════════════════ */
.agent-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--color-bg-page);
}

/* ── 页头 ──────────────────────────────────── */
.agent-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.25rem 2rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  flex-shrink: 0;
}

.agent-page-header-main {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  min-width: 0;
  flex: 1;
}

.agent-back-btn {
  flex-shrink: 0;
  width: 2.25rem;
  height: 2.25rem;
  margin-top: 0.125rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  transition: all 0.2s ease;
}
.agent-back-btn:hover {
  border-color: var(--color-text-secondary);
  color: var(--color-text-primary);
}

.agent-header-text { min-width: 0; }

.agent-breadcrumb {
  margin: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.25rem;
  font-size: 1.125rem;
  font-weight: 600;
}

.agent-breadcrumb-parent {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-text-secondary);
  font-size: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}
.agent-breadcrumb-parent:hover { color: var(--color-text-accent); }

.agent-breadcrumb-sep { color: var(--color-text-tertiary); font-weight: 400; }

.agent-breadcrumb-current {
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-page-header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.625rem;
  flex-shrink: 0;
}

/* ── 加载态 ──────────────────────────────────── */
.agent-page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.625rem;
  flex: 1;
  min-height: 0;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
}
.agent-spinner {
  width: 1.125rem;
  height: 1.125rem;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-text-accent);
  border-radius: 50%;
  animation: agent-spin 0.65s linear infinite;
}
@keyframes agent-spin { to { transform: rotate(360deg); } }

/* ── 主体 ─────────────────────────────────────── */
.agent-page-body {
  flex: 1;
  min-height: 0;
  padding: 1.5rem 2rem 2.5rem;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ── 错误提示 ─────────────────────────────────── */
.agent-error-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  font-size: 0.8125rem;
  color: #dc2626;
  background: rgba(220, 38, 38, 0.05);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: 8px;
}

/* ══════════════════════════════════════════════
   基础信息卡片
══════════════════════════════════════════════ */
.agent-info-card {
  width: 100%;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
}

.agent-card-title {
  margin: 0;
  padding: 1rem 1.5rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
}

/* ── 查看模式：基础信息 ─────────────────────── */
.agent-view-body {
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.agent-view-meta-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.agent-view-meta-row {
  display: flex;
  align-items: baseline;
  gap: 1rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--color-border);
}
.agent-view-meta-row:first-child { padding-top: 0; }
.agent-view-meta-row:last-child { border-bottom: none; padding-bottom: 0; }

.agent-view-meta-key {
  flex-shrink: 0;
  width: 6rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.agent-view-meta-val {
  flex: 1;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  word-break: break-word;
}
.agent-view-meta-val.is-empty { color: var(--color-text-tertiary); }

.agent-view-meta-unit {
  margin-left: 0.25rem;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

.agent-view-divider {
  height: 1px;
  background: var(--color-border);
  margin: 1rem 0;
}

.agent-view-sub-label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.agent-view-prompt-pre {
  margin: 0;
  padding: 0.875rem 1rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.8125rem;
  line-height: 1.65;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 12rem;
  overflow-y: auto;
}

/* ── 状态胶囊条 ─────────────────────────────── */
.agent-status-strip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.875rem 1.5rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-page);
}

.agent-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 999px;
  line-height: 1.4;
  border: 1px solid transparent;
  transition: opacity 0.15s;
}
.agent-status-dot {
  width: 0.4375rem;
  height: 0.4375rem;
  border-radius: 50%;
  flex-shrink: 0;
}

.agent-status-pill.pill-on {
  color: var(--color-text-accent);
  background: color-mix(in srgb, var(--color-text-accent) 10%, var(--color-bg-page));
  border-color: color-mix(in srgb, var(--color-text-accent) 25%, var(--color-border));
}
.agent-status-pill.pill-on .agent-status-dot {
  background: var(--color-text-accent);
}

.agent-status-pill.pill-off {
  color: var(--color-text-tertiary);
  background: var(--color-bg-card);
  border-color: var(--color-border);
}
.agent-status-pill.pill-off .agent-status-dot {
  background: var(--color-text-tertiary);
  opacity: 0.4;
}

.agent-status-pill.pill-locked {
  opacity: 0.55;
}

/* ── 查看模式：RAG KB 标签 ──────────────────── */
.agent-view-kb-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
}

.agent-view-kb-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.6875rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

/* ── 查看模式：模型配置 ─────────────────────── */
.agent-view-model-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.agent-view-model-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

.agent-view-model-key {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.agent-view-model-val {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-primary);
  word-break: break-all;
}
.agent-view-model-val.is-empty {
  color: var(--color-text-tertiary);
  font-weight: 400;
  font-style: italic;
}

/* ── 编辑模式：表单字段 ─────────────────────── */
.agent-edit-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
}

.agent-edit-body .agent-form-field {
  padding: 0;
  gap: 0.375rem;
}

.agent-form-field {
  display: flex;
  flex-direction: column;
  gap: 0.3125rem;
  padding: 0.875rem 1.5rem 0;
}
.agent-form-field:last-of-type { padding-bottom: 0.875rem; }

.agent-form-row2 {
  display: grid;
  grid-template-columns: 9.5rem 1fr;
  gap: 1rem;
  align-items: start;
}
.agent-form-field--compact {
  min-width: 0;
}
.agent-form-input-compact {
  max-width: 9.5rem;
}
.agent-edit-body .agent-form-row2 {
  gap: 1.25rem;
}
.agent-form-row2 .agent-form-field { padding-left: 1.5rem; padding-right: 1.5rem; }
.agent-form-row2 .agent-form-field:first-child { padding-right: 0; }
.agent-form-row2 .agent-form-field:last-child  { padding-left: 0; }
.agent-edit-body .agent-form-row2 .agent-form-field {
  padding: 0;
}

.agent-form-label {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.agent-required { color: #ef4444; }

.agent-form-hint {
  margin-top: 0.125rem;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

.agent-form-label-note {
  font-size: 0.6875rem;
  font-weight: 400;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  padding: 0.1rem 0.375rem;
  border-radius: 4px;
}

.agent-system-prompt {
  width: 100%;
  min-height: 6.5rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.8125rem;
  line-height: 1.65;
  resize: vertical;
  border-radius: 8px;
}

/* ── 功能开关卡片 ─────────────────────────────── */
.agent-switch-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}
.agent-switch-grid.is-compact {
  grid-template-columns: repeat(2, 1fr);
}

.agent-switch-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-height: 5.5rem;
  padding: 0.875rem 1rem;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}
.agent-switch-card:not(.is-off):not(.is-locked) {
  border-color: color-mix(in srgb, var(--color-text-accent) 35%, var(--color-border));
  background: color-mix(in srgb, var(--color-text-accent) 5%, var(--color-bg-page));
}
.agent-switch-card.is-off {
  background: var(--color-bg-card);
}
.agent-switch-card.is-dimmed {
  opacity: 0.65;
}
.agent-switch-card.is-locked {
  opacity: 0.72;
  cursor: not-allowed;
}

.agent-switch-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.agent-switch-card-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.3;
}
.agent-switch-card-hint {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
  line-height: 1.45;
}

/* Toggle */
.agent-toggle {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
  width: 2.125rem;
  height: 1.1875rem;
  cursor: pointer;
}
.agent-toggle input { position: absolute; opacity: 0; width: 0; height: 0; }
.agent-toggle-track {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: var(--color-bg-input);
  border: 1.5px solid var(--color-border);
  transition: background 0.18s, border-color 0.18s;
}
.agent-toggle-track::after {
  content: '';
  position: absolute;
  top: 0.125rem;
  left: 0.125rem;
  width: 0.8125rem;
  height: 0.8125rem;
  border-radius: 50%;
  background: var(--color-text-tertiary);
  transition: transform 0.18s ease, background 0.18s;
}
.agent-toggle input:checked + .agent-toggle-track { background: var(--color-text-accent); border-color: var(--color-text-accent); }
.agent-toggle input:checked + .agent-toggle-track::after { transform: translateX(0.9375rem); background: #fff; }
.agent-toggle.is-on .agent-toggle-track { background: var(--color-text-accent); border-color: var(--color-text-accent); }
.agent-toggle.is-on .agent-toggle-track::after { transform: translateX(0.9375rem); background: #fff; }
.agent-toggle.is-disabled { cursor: not-allowed; opacity: 0.7; pointer-events: none; }

/* ══════════════════════════════════════════════
   Tab 切换栏
══════════════════════════════════════════════ */
.agent-tab-bar {
  display: flex;
  gap: 0;
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-border);
}

.agent-tab-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.625rem 1.125rem;
  font-size: 0.875rem;
  font-weight: 500;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}
.agent-tab-btn:hover { color: var(--color-text-primary); }
.agent-tab-btn.is-active {
  color: var(--color-text-accent);
  border-bottom-color: var(--color-text-accent);
}

.agent-tab-warn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  font-size: 0.625rem;
  font-weight: 700;
  background: #f59e0b;
  color: #fff;
}

/* ── Tab 面板 ──────────────────────────────── */
.agent-tab-panel {
  flex-shrink: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

/* ══════════════════════════════════════════════
   模型配置面板内部
══════════════════════════════════════════════ */
.agent-model-sub {
  padding: 1.125rem 1.5rem;
}
.agent-model-sub:last-child {
  padding-bottom: 1.5rem;
}

.agent-model-sub-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}

.agent-model-sub-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.agent-model-sub-desc {
  margin-left: auto;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

.agent-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.4375rem;
  font-size: 0.6875rem;
  font-weight: 600;
  border-radius: 999px;
}
.agent-badge--required {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.2);
}
.agent-badge--optional {
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
}

.agent-model-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1rem;
  align-items: start;
}

.agent-model-grid :deep(.agent-model-binding-field) {
  padding: 0;
}

.agent-model-grid :deep(.agent-model-binding-field.is-params-expanded) {
  grid-column: 1 / -1;
}

.agent-page.is-embedded .agent-model-grid {
  gap: 0.625rem 0.75rem;
}

/* is-filled: field with a selected value gets accent border */
.agent-form-field.is-filled .form-input,
.agent-form-field.is-filled select.form-input {
  border-color: color-mix(in srgb, var(--color-text-accent) 45%, var(--color-border));
}

.agent-form-field.is-muted { opacity: 0.5; }

.agent-model-divider {
  height: 1px;
  background: var(--color-border);
  margin: 0 1.5rem;
}

/* ══════════════════════════════════════════════
   知识库 & RAG 面板内部
══════════════════════════════════════════════ */
.agent-rag-guide {
  display: flex;
  align-items: flex-start;
  gap: 1.125rem;
  padding: 1.75rem 1.5rem;
}
.agent-rag-guide-icon {
  flex-shrink: 0;
  margin-top: 0.125rem;
  color: var(--color-text-tertiary);
  opacity: 0.6;
}
.agent-rag-guide-text { min-width: 0; }
.agent-rag-guide-title {
  margin: 0 0 0.375rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
}
.agent-rag-guide-desc {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
.agent-rag-guide-desc strong { color: var(--color-text-primary); font-weight: 500; }

.agent-rag-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
}
.agent-rag-row-text {
  display: flex;
  flex-direction: column;
  gap: 0.1875rem;
}
.agent-rag-row-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-primary);
}
.agent-rag-row-hint {
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

/* KB 列表 */
.agent-kb-section { padding: 1rem 1.5rem; }
.agent-kb-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.625rem;
}
.agent-kb-count { font-size: 0.75rem; color: var(--color-text-tertiary); }

.agent-kb-list {
  display: flex;
  flex-direction: column;
  gap: 0.1875rem;
  max-height: 14rem;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.3125rem;
  background: var(--color-bg-page);
}

.agent-kb-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4375rem 0.625rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;
}
.agent-kb-item:hover { background: color-mix(in srgb, var(--color-text-primary) 4%, transparent); }
.agent-kb-item.is-selected { background: color-mix(in srgb, var(--color-text-accent) 8%, var(--color-bg-page)); }
.agent-kb-item-input { display: none; }
.agent-kb-item-check {
  flex-shrink: 0;
  width: 1rem;
  height: 1rem;
  border-radius: 4px;
  border: 1.5px solid var(--color-border);
  background: var(--color-bg-card);
  position: relative;
  transition: background 0.14s, border-color 0.14s;
}
.agent-kb-item.is-selected .agent-kb-item-check { background: var(--color-text-accent); border-color: var(--color-text-accent); }
.agent-kb-item.is-selected .agent-kb-item-check::after {
  content: '';
  position: absolute;
  left: 0.2rem;
  top: 0.05rem;
  width: 0.35rem;
  height: 0.6rem;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.agent-kb-item-name {
  flex: 1;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.agent-kb-item-tag {
  font-size: 0.6875rem;
  color: var(--color-text-tertiary);
  padding: 0.1rem 0.375rem;
  border-radius: 4px;
  background: var(--color-bg-input);
}
.agent-kb-empty {
  margin: 0;
  padding: 0.875rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  text-align: center;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
}

/* ══════════════════════════════════════════════
   向导弹框
══════════════════════════════════════════════ */
.agent-modal-overlay {
  background: rgba(15, 15, 18, 0.45);
  backdrop-filter: blur(4px);
}
.modal-agent-option {
  width: min(920px, 96vw);
  max-width: 920px;
  max-height: 88vh;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(255, 255, 255, 0.04);
  border: 1px solid var(--color-border);
  --agent-gap-xs: 0.25rem;
  --agent-gap-sm: 0.5rem;
  --agent-gap-md: 0.75rem;
  --agent-gap-lg: 1rem;
  --agent-gap-xl: 1.25rem;
  --agent-inset-y: 1.75rem;
  --agent-inset-x: 2.25rem;
  --agent-inset-sidebar: 1.125rem;
}
.agent-modal-header { align-items: flex-start; gap: var(--agent-gap-lg); border-bottom: 1px solid var(--color-border); background: var(--color-bg-card); }
.agent-modal-header-main { flex: 1; min-width: 0; }
.agent-modal-title-row { display: flex; align-items: center; gap: var(--agent-gap-md); flex-wrap: wrap; }
.agent-modal-title-row .modal-title { font-size: 1.0625rem; }
.agent-step-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-accent);
  background: color-mix(in srgb, var(--color-text-accent) 10%, var(--color-bg-page));
  border: 1px solid color-mix(in srgb, var(--color-text-accent) 20%, var(--color-border));
  border-radius: 999px;
  white-space: nowrap;
}
.agent-progress-track { margin-top: var(--agent-gap-md); height: 3px; border-radius: 999px; background: var(--color-bg-input); overflow: hidden; }
.agent-progress-fill { height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--color-button-primary), var(--color-text-accent)); transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1); }
.agent-modal-close { display: flex; align-items: center; justify-content: center; width: 2rem; height: 2rem; padding: 0; font-size: 0; border-radius: 8px; }
.agent-wizard-layout { display: flex; flex: 1; min-height: 0; overflow: hidden; }
.agent-wizard-sidebar {
  flex-shrink: 0; width: 220px;
  display: flex; flex-direction: column; gap: var(--agent-gap-xs);
  padding: var(--agent-inset-y) var(--agent-inset-sidebar);
  border-right: 1px solid var(--color-border);
  background: var(--color-bg-page);
  overflow-y: auto;
}
.agent-wizard-sidebar-item { display: flex; align-items: flex-start; gap: var(--agent-gap-md); padding: var(--agent-gap-sm) var(--agent-gap-md); border-radius: 10px; cursor: pointer; background: none; border: none; text-align: left; width: 100%; transition: background 0.15s; }
.agent-wizard-sidebar-item:not(.is-active):hover { background: color-mix(in srgb, var(--color-text-primary) 5%, transparent); }
.agent-wizard-sidebar-item.is-active { background: color-mix(in srgb, var(--color-text-accent) 10%, var(--color-bg-card)); }
.agent-wizard-sidebar-indicator { flex-shrink: 0; width: 1.625rem; height: 1.625rem; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 0.75rem; font-weight: 600; color: var(--color-text-tertiary); background: var(--color-bg-input); border: 1.5px solid var(--color-border); transition: all 0.2s ease; }
.agent-wizard-sidebar-item.is-active .agent-wizard-sidebar-indicator { color: #fff; background: var(--color-text-accent); border-color: var(--color-text-accent); box-shadow: 0 2px 8px color-mix(in srgb, var(--color-text-accent) 35%, transparent); }
.agent-wizard-sidebar-item.is-done .agent-wizard-sidebar-indicator { color: var(--color-text-accent); border-color: color-mix(in srgb, var(--color-text-accent) 40%, var(--color-border)); background: color-mix(in srgb, var(--color-text-accent) 12%, var(--color-bg-page)); }
.agent-wizard-check { display: flex; align-items: center; justify-content: center; }
.agent-wizard-sidebar-text { display: flex; flex-direction: column; gap: 0.1rem; min-width: 0; }
.agent-wizard-sidebar-label { font-size: 0.8125rem; font-weight: 500; color: var(--color-text-primary); white-space: nowrap; }
.agent-wizard-sidebar-desc { font-size: 0.6875rem; color: var(--color-text-tertiary); overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.agent-option-modal-body { flex: 1; min-width: 0; overflow-y: auto; padding: var(--agent-inset-y) var(--agent-inset-x); }
.agent-step-panel { display: flex; flex-direction: column; gap: var(--agent-gap-xl); }
.agent-step-header { display: flex; flex-direction: column; gap: var(--agent-gap-xs); }
.agent-step-title { font-size: 1rem; font-weight: 600; color: var(--color-text-primary); margin: 0; }
.agent-step-desc { font-size: 0.8125rem; color: var(--color-text-secondary); margin: 0; }
.agent-step-card { display: flex; flex-direction: column; gap: var(--agent-gap-md); }
.agent-step-note { margin: 0; font-size: 0.8125rem; color: var(--color-text-tertiary); padding: 0.5rem 0.75rem; background: var(--color-bg-page); border-left: 3px solid var(--color-border); border-radius: 0 6px 6px 0; }
.agent-step-fields { display: flex; flex-direction: column; gap: 0.875rem; }
.agent-step-fields--compact { margin-top: 0.25rem; }
.agent-field.agent-field-stack { display: flex; flex-direction: column; gap: 0.375rem; }
.agent-step-fields :deep(.agent-model-binding-field) {
  gap: 0.5rem;
}
.agent-field.is-filled .form-input,
.agent-field.is-filled select.form-input {
  border-color: color-mix(in srgb, var(--color-text-accent) 45%, var(--color-border));
}
.agent-field-label { font-size: 0.8125rem; font-weight: 500; color: var(--color-text-secondary); }
.agent-field-input-narrow { max-width: 12rem; }
.agent-switch-group { display: flex; flex-direction: column; border: 1px solid var(--color-border); border-radius: 10px; overflow: hidden; }
.agent-field.agent-field-switch { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.75rem 1rem; border-bottom: 1px solid var(--color-border); }
.agent-field.agent-field-switch:last-child { border-bottom: none; }
.agent-field.agent-field-switch.is-locked { opacity: 0.6; }
.agent-field.agent-field-switch--flat { border: none !important; padding: 0 !important; }
.agent-field-meta { display: flex; flex-direction: column; gap: 0.1rem; }
.agent-field-hint { font-size: 0.75rem; color: var(--color-text-tertiary); }
.agent-switch { position: relative; display: inline-block; width: 2.25rem; height: 1.25rem; flex-shrink: 0; cursor: pointer; }
.agent-switch input { position: absolute; opacity: 0; width: 0; height: 0; }
.agent-switch-track { position: absolute; inset: 0; border-radius: 999px; background: var(--color-bg-input); border: 1.5px solid var(--color-border); transition: background 0.2s, border-color 0.2s; }
.agent-switch-track::after { content: ''; position: absolute; top: 0.1rem; left: 0.1rem; width: 0.875rem; height: 0.875rem; border-radius: 50%; background: var(--color-text-tertiary); transition: transform 0.2s ease, background 0.2s; }
.agent-switch input:checked + .agent-switch-track { background: var(--color-text-accent); border-color: var(--color-text-accent); }
.agent-switch input:checked + .agent-switch-track::after { transform: translateX(1rem); background: #fff; }
.agent-switch.is-on .agent-switch-track { background: var(--color-text-accent); border-color: var(--color-text-accent); }
.agent-switch.is-on .agent-switch-track::after { transform: translateX(1rem); background: #fff; }
.agent-switch.is-disabled { cursor: not-allowed; opacity: 0.7; }
.agent-rag-section { display: flex; flex-direction: column; gap: 0.875rem; padding: 0.875rem 1rem; border: 1px solid var(--color-border); border-radius: 10px; background: var(--color-bg-page); }
.agent-field-block-head { display: flex; align-items: center; justify-content: space-between; }
.agent-prompt-textarea { width: 100%; min-height: 6.5rem; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.8125rem; line-height: 1.65; resize: vertical; border-radius: 10px; }
.agent-form-error { margin-top: 0.625rem; padding: 0.5rem 0.75rem; font-size: 0.8125rem; color: #dc2626; background: rgba(220, 38, 38, 0.06); border: 1px solid rgba(220, 38, 38, 0.18); border-radius: 8px; }
.agent-option-modal-footer { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 1rem 1.5rem; border-top: 1px solid var(--color-border); background: var(--color-bg-card); }
.agent-wizard-prev-placeholder { display: block; width: 4.5rem; }
.agent-wizard-footer-actions { display: flex; align-items: center; gap: 0.625rem; }
.agent-slide-enter-active, .agent-slide-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.agent-slide-enter-from { opacity: 0; transform: translateY(8px); }
.agent-slide-leave-to  { opacity: 0; transform: translateY(-6px); }
.required { color: #dc2626; margin-left: 2px; }

.agent-page.is-embedded {
  height: 100%;
  min-height: 0;
  background: var(--color-bg-card);
}

.agent-embedded-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-shrink: 0;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.agent-embedded-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-embedded-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.agent-page.is-embedded .agent-page-body {
  padding: 0.875rem 1rem 1rem;
  gap: 0.875rem;
}

.agent-page.is-embedded .agent-info-card,
.agent-page.is-embedded .agent-tab-panel {
  border-radius: 8px;
}

.agent-page.is-embedded .refresh-btn,
.agent-page.is-embedded .primary-action-btn {
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
}

/* ── 响应式 ──────────────────────────────────── */
@media (max-width: 680px) {
  .agent-page-header { padding: 1rem 1.25rem; }
  .agent-page-body  { padding: 1rem 1.25rem 2rem; }
  .agent-form-row2  { grid-template-columns: 1fr; }
  .agent-form-row2 .agent-form-field:first-child,
  .agent-form-row2 .agent-form-field:last-child  { padding-left: 0; padding-right: 0; }
  .agent-switch-grid { grid-template-columns: 1fr; }
  .agent-model-grid,
  .agent-view-model-grid { grid-template-columns: 1fr; }
  .agent-model-sub-desc { display: none; }
  .agent-wizard-layout { flex-direction: column; }
  .agent-wizard-sidebar { width: 100%; flex-direction: row; overflow-x: auto; border-right: none; border-bottom: 1px solid var(--color-border); padding: var(--agent-gap-sm) var(--agent-gap-md); gap: var(--agent-gap-xs); }
  .agent-wizard-sidebar-item { flex-direction: column; align-items: center; min-width: 4.25rem; padding: var(--agent-gap-xs) var(--agent-gap-sm); text-align: center; }
  .agent-wizard-sidebar-desc { display: none; }
}
</style>
