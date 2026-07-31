<template>
  <div class="body-content">
        <!-- 文档知识库 -->
        <template v-if="currentSection === 'documents'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">文档知识库</h1>
              <p class="page-desc">上传文档并构建知识库，支持 PDF、Word、TXT、Markdown 格式，供对话检索引用。</p>
            </div>
            <div class="section-header-actions">
              <button
                type="button"
                class="primary-action-btn"
                @click="router.push({ name: 'kbCreate' })"
              >
                新建知识库
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingKbList"
                  @click="loadKnowledgeBaseList"
                >
                  {{ loadingKbList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingKbList }">
                <div v-if="loadingKbList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载知识库列表…</span>
                  </div>
                </div>
                <div v-else-if="knowledgeBaseList.length > 0" class="table-scroll-x">
                  <div class="upload-list kb-list">
                    <div class="list-header">
                      <span>知识库名称</span>
                      <span>描述</span>
                      <span>可见范围</span>
                      <span>文档数</span>
                      <span>创建者</span>
                      <span>创建时间</span>
                      <span>操作</span>
                    </div>
                    <div
                      v-for="kb in knowledgeBaseList"
                      :key="kb.id"
                      class="upload-item kb-item"
                    >
                      <button
                        type="button"
                        class="upload-name kb-name-link"
                        :title="kb.name"
                        @click="goToKbDetail(kb)"
                      >
                        {{ kb.name }}
                      </button>
                      <span class="kb-desc" :title="kb.description || ''">{{ kb.description || '-' }}</span>
                      <span>{{ kb.visibility === 'public' ? '公开' : '私有' }}</span>
                      <span>{{ kb.documentCount }}</span>
                      <span>{{ kb.creatorName || '-' }}</span>
                      <span class="doc-created-at">{{ formatDate(kb.createdAt) }}</span>
                      <div class="upload-actions">
                        <button
                          type="button"
                          class="edit-btn"
                          title="管理知识库"
                          @click="goToKbDetail(kb)"
                        >
                          管理知识库
                        </button>
                        <button
                          v-if="auth.isOwner || kb.memberId === auth.user?.id"
                          type="button"
                          class="delete-btn"
                          title="删除知识库"
                          @click="confirmDeleteKnowledgeBase(kb)"
                        >
                          删除
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无知识库，点击「新建知识库」开始</p>
              </div>
            </div>
          </div>
        </template>

        <!-- API Key 管理 -->
        <template v-else-if="currentSection === 'apikey'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">API Key 管理</h1>
              <p class="page-desc">创建和管理用于调用 OpenAI 兼容接口的 API Key。创建后请妥善保存，完整 Key 仅显示一次。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="showCreateKeyModal = true">
                + 创建 API Key
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingApiKeyList"
                  @click="loadApiKeyList"
                >
                  {{ loadingApiKeyList ? '刷新中…' : '刷新' }}
                </button>
              </div>
            <div class="data-table-panel" :class="{ 'is-loading': loadingApiKeyList }">
              <div v-if="loadingApiKeyList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                <div class="data-table-loading-backdrop" />
                <div class="data-table-loading-content">
                  <span class="data-table-spinner" />
                  <span class="data-table-loading-text">加载 API Key…</span>
                </div>
              </div>
              <div v-else-if="apiKeyList.length > 0" class="table-scroll-x">
                <div class="account-list apikey-list">
                <div class="list-header">
                  <span>名称</span>
                  <span>Key</span>
                  <span>绑定 Agent</span>
                  <span>创建时间</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="key in apiKeyList"
                  :key="key.id"
                  class="account-item apikey-item"
                >
                  <div class="account-info apikey-info">
                    <span class="account-username">{{ key.name || '未命名' }}</span>
                    <span class="apikey-masked">{{ key.apiKey }}</span>
                    <span class="apikey-agent">
                      <select
                        class="form-input apikey-agent-select"
                        :value="key.chatAgentOptionId ?? ''"
                        :disabled="updatingKeyIds.has(key.id)"
                        @change="onKeyAgentChange(key, $event)"
                      >
                        <option value="">未绑定</option>
                        <option v-for="agent in agentOptionList" :key="agent.id" :value="agent.id">
                          {{ agent.name }}
                        </option>
                      </select>
                    </span>
                    <span class="apikey-meta">
                      {{ formatDate(key.createdAt) }}
                      <template v-if="key.expiresAt"> · 过期 {{ formatDate(key.expiresAt) }}</template>
                    </span>
                  </div>
                  <div class="upload-actions">
                    <button
                      type="button"
                      class="delete-btn"
                      @click="confirmDeleteKey(key)"
                    >
                      删除
                    </button>
                  </div>
                </div>
                </div>
              </div>
              <p v-else class="table-empty">暂无 API Key，点击上方按钮创建</p>
            </div>
          </div>
        </div>

          <!-- 创建 API Key 弹窗 -->
          <div v-if="showCreateKeyModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">创建 API Key</h2>
                <button type="button" class="modal-close" @click="closeCreateKeyModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">名称（可选）</label>
                  <input
                    v-model="createKeyForm.name"
                    type="text"
                    class="form-input"
                    placeholder="例如：生产环境"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">有效天数（可选，不填则永不过期）</label>
                  <input
                    v-model.number="createKeyForm.expiresInDays"
                    type="number"
                    class="form-input"
                    placeholder="例如：30"
                    min="1"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">绑定 Agent（可选，多模态图片识别必需）</label>
                  <select v-model="createKeyForm.chatAgentOptionId" class="form-input">
                    <option :value="null">不绑定</option>
                    <option v-for="agent in agentOptionList" :key="agent.id" :value="agent.id">
                      {{ agent.name }}
                    </option>
                  </select>
                </div>
                <p v-if="createKeyError" class="form-error">{{ createKeyError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeCreateKeyModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="creatingKey" @click="submitCreateKey">
                  {{ creatingKey ? '创建中...' : '创建' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 新建 Key 展示弹窗（仅创建成功后显示一次） -->
          <div v-if="newKeyResult" class="modal-overlay">
            <div class="modal-content modal-key-result">
              <div class="modal-header">
                <h2 class="modal-title">API Key 已创建，请妥善保存</h2>
                <button type="button" class="modal-close" @click="closeNewKeyModal">×</button>
              </div>
              <div class="modal-body">
                <p class="form-label">完整 Key 仅显示一次，请复制保存：</p>
                <div class="key-display">
                  <code class="key-value">{{ newKeyResult.apiKey }}</code>
                  <button type="button" class="copy-key-btn" @click="copyNewKey">复制</button>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-confirm" @click="closeNewKeyModal">已保存，关闭</button>
              </div>
            </div>
          </div>
        </template>

        <!-- 外部服务配置（高德 Key 等） -->
        <template v-else-if="currentSection === 'bizConfig'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">外部服务配置</h1>
              <p class="page-desc">每条记录对应一个「业务配置键」；其下可添加多组子键/子值，保存时合并为 JSON 字符串写入数据库。可选加密存储；列表不展示配置内容，编辑时加载各子键名（子值需重新填写）。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="openBizConfigModal(null)">
                + 添加配置
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingBizConfigList"
                  @click="loadBizConfigList"
                >
                  {{ loadingBizConfigList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingBizConfigList }">
                <div v-if="loadingBizConfigList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载外部服务配置…</span>
                  </div>
                </div>
                <div v-else-if="bizConfigList.length > 0" class="table-scroll-x">
                  <div class="account-list biz-config-list">
                  <div class="list-header">
                    <span>配置键</span>
                    <span>备注</span>
                    <span>创建者</span>
                    <span>更新时间</span>
                    <span>操作</span>
                  </div>
                  <div
                    v-for="row in bizConfigList"
                    :key="row.id"
                    class="account-item biz-config-item"
                  >
                    <div class="account-info biz-config-info">
                      <span class="biz-config-key" :title="row.configKey">{{ row.configKey }}</span>
                      <span class="biz-config-remark">{{ row.remark || '—' }}</span>
                      <span>{{ row.creatorName || '-' }}</span>
                      <span class="apikey-meta">{{ formatDate(row.updatedAt) }}</span>
                    </div>
                    <div class="upload-actions">
                      <button
                        type="button"
                        class="edit-btn"
                        @click="openBizConfigModal(row)"
                      >{{ auth.isOwner || row.memberId === auth.user?.id ? '编辑' : '预览' }}</button>
                      <button
                        v-if="auth.isOwner || row.memberId === auth.user?.id"
                        type="button"
                        class="delete-btn"
                        @click="confirmDeleteBizConfig(row)"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无外部服务配置，点击上方「添加配置」</p>
              </div>
            </div>
          </div>

          <div v-if="showBizConfigModal" class="modal-overlay">
            <div class="modal-content modal-wide">
              <div class="modal-header">
                <h2 class="modal-title">{{ bizConfigReadonly ? '预览外部服务配置' : bizConfigEditing ? '编辑外部服务配置' : '添加外部服务配置' }}</h2>
                <button type="button" class="modal-close" @click="closeBizConfigModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">业务配置键 <span class="required">*</span></label>
                  <input
                    v-model="bizConfigForm.configKey"
                    type="text"
                    class="form-input"
                    :disabled="!!bizConfigEditing || bizConfigReadonly"
                    placeholder="例如：amap.credentials（库中一条记录的主键）"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">配置项（子键 / 子值）<span class="required">*</span></label>
                  <p class="form-hint">至少保留一行；空行会被忽略。保存时将多行合并为 JSON 对象写入「配置值」字段。</p>
                  <div class="property-list biz-config-pair-list">
                    <div class="biz-config-pair-header">
                      <span>子键</span>
                      <span>子值</span>
                      <span v-if="!bizConfigReadonly" class="biz-config-pair-header-spacer" />
                    </div>
                    <div
                      v-for="(pair, index) in bizConfigPairs"
                      :key="index"
                      class="property-row biz-config-pair-row"
                    >
                      <input
                        v-model="pair.key"
                        type="text"
                        class="form-input property-input"
                        :disabled="bizConfigReadonly"
                        placeholder="如 webServiceKey"
                      />
                      <input
                        v-model="pair.value"
                        type="text"
                        class="form-input property-input"
                        :disabled="bizConfigReadonly"
                        placeholder="对应的值"
                      />
                      <button
                        v-if="!bizConfigReadonly"
                        type="button"
                        class="property-remove-btn"
                        :disabled="bizConfigPairs.length <= 1"
                        title="删除此行"
                        @click="removeBizConfigPair(index)"
                      >
                        ×
                      </button>
                    </div>
                    <button v-if="!bizConfigReadonly" type="button" class="property-add-btn" @click="addBizConfigPair">
                      <span>＋</span> 添加配置项
                    </button>
                  </div>
                  <p v-if="bizConfigEditing && loadingBizConfigDetail" class="form-hint">
                    正在加载配置值…
                  </p>
                </div>
                <div class="form-group">
                  <label class="form-label">备注</label>
                  <input
                    v-model="bizConfigForm.remark"
                    type="text"
                    class="form-input"
                    :disabled="bizConfigReadonly"
                    placeholder="可选，如：高德 Web 服务"
                  />
                </div>
                <p v-if="bizConfigFormError" class="form-error">{{ bizConfigFormError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeBizConfigModal">{{ bizConfigReadonly ? '关闭' : '取消' }}</button>
                <button v-if="!bizConfigReadonly" type="button" class="btn-confirm" :disabled="submittingBizConfig" @click="submitBizConfig">
                  {{ submittingBizConfig ? '保存中…' : '保存' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- 账号管理 -->
        <template v-else-if="currentSection === 'accounts'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">账号管理</h1>
              <p class="page-desc">
                <template v-if="auth.isOwner">
                  查看团队账号结构，管理 Member，并可生成分享码供他人注册加入。
                </template>
                <template v-else>
                  查看当前团队信息，Member 共享 Owner 的模型配置与资源。
                </template>
              </p>
            </div>
            <div v-if="auth.isOwner" class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="openAddAccountModal">
                + 添加成员
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingMemberList"
                  @click="loadTeamOverview"
                >
                  {{ loadingMemberList ? '刷新中…' : '刷新' }}
                </button>
              </div>
            <div class="data-table-panel" :class="{ 'is-loading': loadingMemberList }">
              <div class="team-block-header">
                <span class="team-block-name">{{ teamOverview?.teamName || '未命名团队' }}</span>
                <button
                  v-if="auth.isOwner"
                  type="button"
                  class="team-edit-btn"
                  @click="openEditTeamModal"
                >
                  编辑团队
                </button>
              </div>
              <div v-if="loadingMemberList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                <div class="data-table-loading-backdrop" />
                <div class="data-table-loading-content">
                  <span class="data-table-spinner" />
                  <span class="data-table-loading-text">加载团队账号…</span>
                </div>
              </div>
              <div v-else-if="displayAccountRows.length > 0" class="table-scroll-x">
                <div class="account-list accounts-list">
                <div class="list-header">
                  <span>用户名</span>
                  <span>身份</span>
                  <span>昵称</span>
                  <span>手机号</span>
                  <span>状态</span>
                  <span>创建时间</span>
                  <span>操作</span>
                </div>
                <div
                  v-for="account in displayAccountRows"
                  :key="account.id"
                  class="account-item"
                  :class="{ 'is-current-user': account.id === teamOverview?.currentUserId }"
                >
                  <div class="account-info">
                    <span class="account-username">
                      {{ account.username }}
                      <span v-if="account.id === teamOverview?.currentUserId" class="current-user-inline">当前</span>
                    </span>
                    <span class="role-badge" :class="roleBadgeClass(account.role)">{{ roleLabel(account.role) }}</span>
                    <span class="account-nickname">{{ account.nickname || '-' }}</span>
                    <span class="account-phone">{{ account.phone || '-' }}</span>
                    <span class="account-status" :class="statusBadgeClass(account.status)">
                      {{ statusLabel(account.status) }}
                    </span>
                    <span class="account-created">{{ formatDate(account.createdAt ?? null) }}</span>
                  </div>
                  <div class="upload-actions">
                    <template v-if="account.role === MEMBER_ROLE.OWNER">
                      <span class="current-user-tag">—</span>
                    </template>
                    <template v-else-if="auth.isOwner">
                      <button type="button" class="edit-btn" @click="openEditAccountModal(account)">编辑</button>
                      <button
                        v-if="account.status === MEMBER_STATUS.ENABLED"
                        type="button"
                        class="delete-btn"
                        @click="confirmDisableAccount(account)"
                      >
                        禁用
                      </button>
                      <button
                        v-else
                        type="button"
                        class="edit-btn"
                        @click="enableAccount(account)"
                      >
                        启用
                      </button>
                      <button
                        type="button"
                        class="delete-btn"
                        @click="confirmDelete(account)"
                      >
                        删除
                      </button>
                    </template>
                    <template v-else>
                      <span class="current-user-tag">只读</span>
                    </template>
                  </div>
                </div>
                </div>
              </div>
              <p v-else class="table-empty">暂无团队账号信息</p>
            </div>
          </div>
        </div>

          <!-- 添加成员弹窗 -->
          <div v-if="showAddAccountModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">添加成员</h2>
                <button type="button" class="modal-close" @click="closeAddAccountModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">用户名 <span class="required">*</span></label>
                  <input
                    v-model="addAccountForm.username"
                    type="text"
                    class="form-input"
                    placeholder="请输入用户名"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">密码 <span class="required">*</span></label>
                  <input
                    v-model="addAccountForm.password"
                    type="password"
                    class="form-input"
                    placeholder="请输入密码"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">昵称</label>
                  <input
                    v-model="addAccountForm.nickname"
                    type="text"
                    class="form-input"
                    placeholder="请输入昵称（可选）"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">手机号</label>
                  <input
                    v-model="addAccountForm.phone"
                    type="tel"
                    class="form-input"
                    placeholder="请输入手机号（可选）"
                  />
                </div>
                <p v-if="addAccountError" class="form-error">{{ addAccountError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeAddAccountModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="addingAccount" @click="submitAddAccount">
                  {{ addingAccount ? '添加中...' : '确定' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 编辑团队弹窗 -->
          <div v-if="showEditTeamModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">编辑团队</h2>
                <button type="button" class="modal-close" @click="closeEditTeamModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">团队名称 <span class="required">*</span></label>
                  <input
                    v-model="editTeamForm.name"
                    type="text"
                    class="form-input"
                    placeholder="请输入团队名称"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">默认对话模型</label>
                  <select v-model="editTeamForm.defaultChatModelId" class="form-input">
                    <option :value="null">不设置</option>
                    <option
                      v-for="m in modelKeyList.filter(k => k.keyType === 'chat')"
                      :key="m.id"
                      :value="m.id"
                    >
                      {{ m.modelName }}（{{ m.modelProvider }}）
                    </option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">分享码</label>
                  <div class="share-code-field">
                    <code class="share-code-value">{{ editTeamForm.shareCode || '未生成' }}</code>
                    <button
                      v-if="editTeamForm.shareCode"
                      type="button"
                      class="secondary-action-btn"
                      @click="copyShareCode"
                    >
                      复制
                    </button>
                    <button
                      type="button"
                      class="secondary-action-btn"
                      @click="onGenerateShareCode"
                    >
                      {{ editTeamForm.shareCode ? '重新生成' : '生成分享码' }}
                    </button>
                  </div>
                  <p class="form-hint">分享码不可手动编辑；点击生成后需点「确定」才会保存生效。</p>
                </div>
                <p v-if="editTeamError" class="form-error">{{ editTeamError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeEditTeamModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="editTeamSaving" @click="submitEditTeam">
                  {{ editTeamSaving ? '保存中...' : '确定' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 编辑成员弹窗 -->
          <div v-if="showEditAccountModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">编辑成员</h2>
                <button type="button" class="modal-close" @click="closeEditAccountModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">用户名</label>
                  <input
                    :value="editingAccount?.username"
                    type="text"
                    class="form-input"
                    disabled
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">昵称</label>
                  <input
                    v-model="editAccountForm.nickname"
                    type="text"
                    class="form-input"
                    placeholder="请输入昵称"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">手机号</label>
                  <input
                    v-model="editAccountForm.phone"
                    type="tel"
                    class="form-input"
                    placeholder="请输入手机号"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">状态</label>
                  <select v-model.number="editAccountForm.status" class="form-input">
                    <option :value="MEMBER_STATUS.ENABLED">正常</option>
                    <option :value="MEMBER_STATUS.DISABLED">禁用</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">新密码</label>
                  <input
                    v-model="editAccountForm.password"
                    type="password"
                    class="form-input"
                    placeholder="留空则不修改"
                    autocomplete="new-password"
                  />
                </div>
                <p v-if="editAccountError" class="form-error">{{ editAccountError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeEditAccountModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="editingAccountSaving" @click="submitEditAccount">
                  {{ editingAccountSaving ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </div>

        </template>

        <!-- Agent 配置 -->
        <template v-else-if="currentSection === 'agentOption'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">Agent 配置</h1>
              <p class="page-desc">管理对话 Agent 的行为配置，包括 RAG、工具调用、消息窗口及各类模型绑定。对话页可选择已启用的配置。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="router.push({ name: 'agentOptionCreate' })">
                + 添加配置
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingAgentOptionList"
                  @click="loadAgentOptionList"
                >
                  {{ loadingAgentOptionList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingAgentOptionList }">
                <div v-if="loadingAgentOptionList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载 Agent 配置…</span>
                  </div>
                </div>
                <div v-else-if="agentOptionList.length > 0" class="table-scroll-x">
                  <div class="account-list agent-option-list">
                    <div class="list-header">
                      <span>名称</span>
                      <span>状态</span>
                      <span>RAG</span>
                      <span>工具</span>
                      <span>消息窗口</span>
                      <span>更新时间</span>
                      <span>操作</span>
                    </div>
                    <div
                      v-for="item in agentOptionList"
                      :key="item.id"
                      class="account-item agent-option-item"
                    >
                      <div class="account-info agent-option-info">
                        <span class="agent-option-name">{{ item.name }}</span>
                        <span class="agent-option-flag" :class="{ 'is-on': item.enabled }">
                          {{ item.enabled ? '启用' : '禁用' }}
                        </span>
                        <span class="agent-option-flag" :class="{ 'is-on': item.rag }">
                          {{ item.rag ? '开' : '关' }}
                        </span>
                        <span class="agent-option-flag" :class="{ 'is-on': item.tools }">
                          {{ item.tools ? '开' : '关' }}
                        </span>
                        <span>{{ item.maxMessages ?? '—' }}</span>
                        <span class="apikey-meta">{{ formatDate(item.updatedAt ?? null) }}</span>
                      </div>
                      <div class="upload-actions">
                        <button type="button" class="edit-btn" @click="router.push({ name: 'agentOptionDetail', params: { id: item.id } })">编辑</button>
                        <button type="button" class="delete-btn" @click="confirmDeleteAgentOption(item)">删除</button>
                      </div>
                    </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无 Agent 配置，点击上方按钮添加</p>
              </div>
            </div>
          </div>
        </template>

        <!-- 模型配置 -->
        <template v-else-if="currentSection === 'modelConfig'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">模型配置</h1>
              <p class="page-desc">配置第三方模型的 API Key，支持 OpenAI、Anthropic、DeepSeek、Qwen 等模型提供商。</p>
            </div>
            <div class="section-header-actions">
              <button v-if="auth.isOwner" type="button" class="primary-action-btn" @click="showCreateModelModal = true">
                + 添加模型
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingModelKeyList"
                  @click="loadModelKeyList"
                >
                  {{ loadingModelKeyList ? '刷新中…' : '刷新' }}
                </button>
              </div>
            <div class="data-table-panel" :class="{ 'is-loading': loadingModelKeyList }">
              <div v-if="loadingModelKeyList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                <div class="data-table-loading-backdrop" />
                <div class="data-table-loading-content">
                  <span class="data-table-spinner" />
                  <span class="data-table-loading-text">加载模型配置…</span>
                </div>
              </div>
              <div v-else-if="modelKeyList.length > 0" class="table-scroll-x">
                <table class="model-table">
                  <thead>
                    <tr>
                      <th>模型名称</th>
                      <th>提供商</th>
                      <th>类型</th>
                      <th>描述</th>
                      <th>Key</th>
                      <th>状态</th>
                      <th>创建时间</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in modelKeyList" :key="item.id">
                      <td class="col-model-name">{{ item.modelName }}</td>
                      <td class="col-provider">
                        <span class="model-provider-tag" :class="providerClass(item.modelProvider)">{{ item.modelProvider }}</span>
                      </td>
                      <td class="col-type">
                        <span class="model-type-tag" :class="keyTypeClass(item.keyType)">{{ keyTypeLabel(item.keyType) }}</span>
                      </td>
                      <td class="col-description">{{ item.description || '-' }}</td>
                      <td class="col-key">{{ item.maskedApiKey }}</td>
                      <td class="col-status">
                        <span class="model-status-badge" :class="{ 'is-enabled': item.enabled, 'is-disabled': !item.enabled }">
                          {{ item.enabled ? '上线' : '下线' }}
                        </span>
                      </td>
                      <td class="col-time">{{ formatDate(item.createdAt) }}</td>
                      <td class="col-actions">
                        <button
                          v-if="auth.isOwner"
                          type="button"
                          class="edit-btn"
                          @click="openEditModelModal(item)"
                        >
                          编辑
                        </button>
                        <button
                          v-if="auth.isOwner"
                          type="button"
                          class="toggle-btn"
                          :class="item.enabled ? 'btn-disable' : 'btn-enable'"
                          @click="toggleModelKeyEnabledStatus(item)"
                        >
                          {{ item.enabled ? '下线' : '上线' }}
                        </button>
                        <button
                          v-if="auth.isOwner"
                          type="button"
                          class="delete-btn"
                          @click="confirmDeleteModelKey(item)"
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="table-empty">{{ auth.isOwner ? '暂无模型配置，点击上方按钮添加' : '暂无模型配置' }}</p>
            </div>
          </div>
        </div>

          <!-- 创建模型 API Key 弹窗 -->
          <div v-if="showCreateModelModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">添加模型</h2>
                <button type="button" class="modal-close" @click="closeCreateModelModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">模型名称 <span class="required">*</span></label>
                  <input
                    v-model="createModelForm.modelName"
                    type="text"
                    class="form-input"
                    placeholder="例如：gpt-4o、claude-sonnet-4-20250514"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">模型提供商 <span class="required">*</span></label>
                  <select v-model="createModelForm.modelProvider" class="form-input">
                    <option value="" disabled>请选择提供商</option>
                    <option v-for="p in providerOptions" :key="p" :value="p">{{ p }}</option>
                  </select>
                </div>
                <div v-if="createModelForm.modelProvider === '自定义'" class="form-group">
                  <label class="form-label">自定义提供商名称 <span class="required">*</span></label>
                  <input
                    v-model="createModelForm.customProvider"
                    type="text"
                    class="form-input"
                    placeholder="请输入提供商名称"
                  />
                </div>
                <div class="form-group">
                  <label class="form-label">模型类型 <span class="required">*</span></label>
                  <select v-model="createModelForm.keyType" class="form-input">
                    <option value="" disabled>请选择模型类型</option>
                    <option value="chat">对话（CHAT）</option>
                    <option value="streaming_chat">流式对话（STREAMING_CHAT）</option>
                    <option value="embedding">嵌入模型（EMBEDDING）</option>
                    <option value="scoring">评分模型（SCORING）</option>
                    <option value="moderation">审核模型（MODERATION）</option>
                    <option value="image">图像模型（IMAGE）</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">模型描述（可选）</label>
                  <textarea
                    v-model="createModelForm.description"
                    class="form-input form-textarea"
                    rows="3"
                    placeholder="请输入模型描述、能力说明等"
                  />
                </div>
                <div v-if="isChatKeyType(createModelForm.keyType)" class="form-group">
                  <label class="form-label">模型能力</label>
                  <label class="capability-toggle">
                    <input
                      type="checkbox"
                      v-model="createModelForm.visionSupported"
                    />
                    支持视觉理解
                  </label>
                </div>
                <div class="form-group">
                  <label class="form-label">API Key <span class="required">*</span></label>
                  <input
                    v-model="createModelForm.apiKey"
                    type="password"
                    class="form-input"
                    placeholder="请输入 API Key"
                  />
                </div>
                <p v-if="createModelError" class="form-error">{{ createModelError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeCreateModelModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="creatingModelKey" @click="submitCreateModelKey">
                  {{ creatingModelKey ? '添加中...' : '确定' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 编辑模型弹窗（仅允许编辑描述与能力） -->
          <div v-if="showEditModelModal" class="modal-overlay">
            <div class="modal-content">
              <div class="modal-header">
                <h2 class="modal-title">编辑模型</h2>
                <button type="button" class="modal-close" @click="closeEditModelModal">×</button>
              </div>
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">模型名称</label>
                  <input :value="editingModelKey?.modelName" type="text" class="form-input" disabled />
                </div>
                <div class="form-group">
                  <label class="form-label">模型提供商</label>
                  <input :value="editingModelKey?.modelProvider" type="text" class="form-input" disabled />
                </div>
                <div class="form-group">
                  <label class="form-label">模型类型</label>
                  <input :value="editingModelKey ? keyTypeLabel(editingModelKey.keyType) : ''" type="text" class="form-input" disabled />
                </div>
                <div class="form-group">
                  <label class="form-label">模型描述</label>
                  <textarea
                    v-model="editModelForm.description"
                    class="form-input form-textarea"
                    rows="3"
                    placeholder="请输入模型描述、能力说明等"
                  />
                </div>
                <div v-if="editingModelKey && isChatKeyType(editingModelKey.keyType)" class="form-group">
                  <label class="form-label">模型能力</label>
                  <label class="capability-toggle">
                    <input type="checkbox" v-model="editModelForm.visionSupported" />
                    支持视觉理解
                  </label>
                </div>
                <p v-if="editModelError" class="form-error">{{ editModelError }}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn-cancel" @click="closeEditModelModal">取消</button>
                <button type="button" class="btn-confirm" :disabled="savingModelKey" @click="submitEditModelKey">
                  {{ savingModelKey ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- Function Call 管理 -->
        <template v-else-if="currentSection === 'functionCall'">
          <div class="section-header-bar">
            <div class="section-header-info">
              <h1 class="page-title">Function Call</h1>
              <p class="page-desc">管理 AI 对话中可调用的工具/函数，定义工具的参数、描述和执行脚本。</p>
            </div>
            <div class="section-header-actions">
              <button type="button" class="primary-action-btn" @click="goCreateFunctionCall">
                + 添加工具
              </button>
            </div>
          </div>

          <div class="section-body">
            <div class="data-table-card">
              <div class="data-table-toolbar">
                <button
                  type="button"
                  class="refresh-btn"
                  :disabled="loadingFunctionCallList"
                  @click="loadFunctionCallList"
                >
                  {{ loadingFunctionCallList ? '刷新中…' : '刷新' }}
                </button>
              </div>
              <div class="data-table-panel" :class="{ 'is-loading': loadingFunctionCallList }">
                <div v-if="loadingFunctionCallList" class="data-table-loading-overlay" aria-busy="true" aria-live="polite">
                  <div class="data-table-loading-backdrop" />
                  <div class="data-table-loading-content">
                    <span class="data-table-spinner" />
                    <span class="data-table-loading-text">加载工具列表…</span>
                  </div>
                </div>
                <div v-else-if="functionCallList.length > 0" class="table-scroll-x">
                  <div class="account-list fc-list">
                  <div class="list-header">
                    <span>工具名称</span>
                    <span>工具描述</span>
                    <span>创建者</span>
                    <span>创建时间</span>
                    <span>操作</span>
                  </div>
                  <div
                    v-for="item in functionCallList"
                    :key="item.id"
                    class="account-item fc-item"
                  >
                    <div class="account-info fc-info">
                      <span class="fc-name">{{ item.name }}</span>
                      <span class="fc-desc" :title="item.description">{{ item.description }}</span>
                      <span>{{ item.creatorName || '-' }}</span>
                      <span class="apikey-meta">{{ formatDate(item.createdAt) }}</span>
                    </div>
                    <div class="upload-actions">
                      <button
                        type="button"
                        class="edit-btn"
                        @click="router.push({ name: 'functionCallDetail', params: { id: item.id } })"
                      >
                        {{ auth.isOwner || item.memberId === auth.user?.id ? '编辑' : '预览' }}
                      </button>
                      <button
                        v-if="auth.isOwner || item.memberId === auth.user?.id"
                        type="button"
                        class="delete-btn"
                        @click="confirmDeleteFunctionCall(item)"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                  </div>
                </div>
                <p v-else class="table-empty">暂无工具，点击上方按钮添加</p>
              </div>
            </div>
          </div>

        </template>

    <!-- 确认删除弹框 -->
    <div v-if="confirmDialog.visible" class="modal-overlay">
      <div class="modal-content modal-confirm">
        <div class="modal-header">
          <h2 class="modal-title">{{ confirmDialog.title }}</h2>
          <button type="button" class="modal-close" @click="cancelConfirm">×</button>
        </div>
        <div class="modal-body">
          <p class="confirm-message">{{ confirmDialog.message }}</p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="cancelConfirm">取消</button>
          <button type="button" class="btn-confirm btn-danger" @click="executeConfirm">确定删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { MEMBER_ROLE, MEMBER_STATUS, deleteMember } from '@/api/auth'
import type { KnowledgeBaseItem } from '@/api/knowledgeBase'
import { getKnowledgeBaseList, deleteKnowledgeBase } from '@/api/knowledgeBase'
import {
  getTeamOverview,
  createTeamMember,
  updateTeamMember,
  updateTeam,
  disableTeamMember,
  roleBadgeClass,
  roleLabel,
  statusBadgeClass,
  statusLabel,
} from '@/api/team'
import type { TeamMember, TeamOverview } from '@/api/team'
import { getApiKeyList, createApiKey, updateApiKey, disableApiKey, deleteApiKey } from '@/api/apikey'
import type { ApiKeyListItem, CreateApiKeyResult } from '@/api/apikey'
import { getModelKeyList, createModelKey, deleteModelKey, toggleModelKeyEnabled, updateModelKey } from '@/api/model'
import type { ModelApiKeyItem } from '@/api/model'
import { getFunctionCallList, deleteFunctionCall } from '@/api/functioncall'
import type { FunctionCallItem } from '@/api/functioncall'
import { getBizConfigList, getBizConfigItem, saveBizConfigItem, deleteBizConfigItem } from '@/api/config'
import type { BizConfigItem } from '@/api/config'
import {
  getAgentOptionList,
  deleteAgentOption,
} from '@/api/agentOption'
import type { AgentOptionItem } from '@/api/agentOption'
import { toastError, toastSuccess } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuItems = [
  { id: 'documents', label: '文档知识库' },
  { id: 'functionCall', label: 'Function Call' },
  { id: 'agentOption', label: 'Agent 配置' },
  { id: 'apikey', label: 'API Key 管理' },
  { id: 'bizConfig', label: '外部服务配置' },
  { id: 'modelConfig', label: '模型配置' },
  { id: 'accounts', label: '账号管理' },
]

const currentSection = ref<string>('documents')

const knowledgeBaseList = ref<KnowledgeBaseItem[]>([])
const loadingKbList = ref(false)
const loadingApiKeyList = ref(false)
const loadingMemberList = ref(false)
const loadingModelKeyList = ref(false)
const loadingFunctionCallList = ref(false)
const loadingBizConfigList = ref(false)
const loadingAgentOptionList = ref(false)
const agentOptionList = ref<AgentOptionItem[]>([])
const bizConfigList = ref<BizConfigItem[]>([])
const showBizConfigModal = ref(false)
const bizConfigEditing = ref<BizConfigItem | null>(null)
const submittingBizConfig = ref(false)
const loadingBizConfigDetail = ref(false)
const bizConfigFormError = ref('')
const bizConfigReadonly = computed(
  () => auth.isMember && bizConfigEditing.value != null && bizConfigEditing.value.memberId !== auth.user?.id,
)
interface BizConfigPairRow {
  key: string
  value: string
}

const bizConfigForm = ref({
  configKey: '',
  remark: '',
})

/** 弹窗内多行子键/子值，提交时 JSON.stringify 合并为 configValue */
const bizConfigPairs = ref<BizConfigPairRow[]>([{ key: '', value: '' }])

// 账号管理相关
const teamOverview = ref<TeamOverview | null>(null)
const showAddAccountModal = ref(false)
const showEditAccountModal = ref(false)
const showEditTeamModal = ref(false)
const addingAccount = ref(false)
const editingAccountSaving = ref(false)
const editTeamSaving = ref(false)
const addAccountError = ref('')
const editAccountError = ref('')
const editTeamError = ref('')
const editingAccount = ref<TeamMember | null>(null)
const editTeamForm = ref<{ name: string; defaultChatModelId: number | null; shareCode: string }>({
  name: '',
  defaultChatModelId: null,
  shareCode: '',
})
const addAccountForm = ref({
  username: '',
  password: '',
  nickname: '',
  phone: '',
})
const editAccountForm = ref({
  nickname: '',
  phone: '',
  status: MEMBER_STATUS.ENABLED,
  password: '',
})

const displayAccountRows = computed(() => {
  if (!teamOverview.value) return []
  const owner = {
    ...teamOverview.value.owner,
    role: teamOverview.value.owner.role ?? MEMBER_ROLE.OWNER,
  }
  const members = teamOverview.value.members.map((member) => ({
    ...member,
    role: member.role ?? MEMBER_ROLE.MEMBER,
  }))
  return [owner, ...members]
})

// API Key 管理
const apiKeyList = ref<ApiKeyListItem[]>([])
const showCreateKeyModal = ref(false)
const creatingKey = ref(false)
const createKeyError = ref('')
const createKeyForm = ref<{
  name: string
  expiresInDays: number | undefined
  chatAgentOptionId: number | null
}>({ name: '', expiresInDays: undefined, chatAgentOptionId: null })
const newKeyResult = ref<CreateApiKeyResult | null>(null)
const updatingKeyIds = ref<Set<number>>(new Set())

// 模型配置相关
const modelKeyList = ref<ModelApiKeyItem[]>([])
const showCreateModelModal = ref(false)
const creatingModelKey = ref(false)
const createModelError = ref('')
const createModelForm = ref({
  modelName: '',
  modelProvider: '',
  customProvider: '',
  keyType: 'chat',
  description: '',
  apiKey: '',
  visionSupported: false,
})
const providerOptions = ['OpenAI', 'Anthropic', 'DeepSeek', 'Qwen', 'Ollama', '自定义']

// 编辑模型
const showEditModelModal = ref(false)
const savingModelKey = ref(false)
const editModelError = ref('')
const editingModelKey = ref<ModelApiKeyItem | null>(null)
const editModelForm = ref({
  description: '',
  visionSupported: false,
})

// Function Call 管理
const functionCallList = ref<FunctionCallItem[]>([])

// 确认弹框
const confirmDialog = ref({
  visible: false,
  title: '确认删除',
  message: '',
  onConfirm: null as (() => void) | null,
})

function showConfirm(message: string, onConfirm: () => void) {
  confirmDialog.value = { visible: true, title: '确认删除', message, onConfirm }
}

function cancelConfirm() {
  confirmDialog.value.visible = false
  confirmDialog.value.onConfirm = null
}

function executeConfirm() {
  confirmDialog.value.onConfirm?.()
  confirmDialog.value.visible = false
  confirmDialog.value.onConfirm = null
}

async function loadKnowledgeBaseList() {
  loadingKbList.value = true
  try {
    const res = await getKnowledgeBaseList()
    if (res.success && Array.isArray(res.data)) {
      knowledgeBaseList.value = res.data
    }
  } catch {
    // ignore
  } finally {
    loadingKbList.value = false
  }
}

/** 进入知识库管理页（默认基础信息） */
function goToKbDetail(kb: KnowledgeBaseItem) {
  router.push({ name: 'kbDetail', params: { kbId: kb.id } })
}

function confirmDeleteKnowledgeBase(kb: KnowledgeBaseItem) {
  showConfirm(
    `确定要删除知识库「${kb.name}」吗？其下所有文档与向量数据将一并删除。`,
    () => deleteKnowledgeBaseById(kb.id),
  )
}

async function deleteKnowledgeBaseById(id: number) {
  try {
    const res = await deleteKnowledgeBase(id)
    if (res.success) {
      knowledgeBaseList.value = knowledgeBaseList.value.filter((k) => k.id !== id)
    } else {
      toastError(res.message || '删除失败')
    }
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

// 账号管理相关方法
function openAddAccountModal() {
  showAddAccountModal.value = true
}

function closeAddAccountModal() {
  showAddAccountModal.value = false
  addAccountForm.value = {
    username: '',
    password: '',
    nickname: '',
    phone: '',
  }
  addAccountError.value = ''
}

function openEditTeamModal() {
  editTeamForm.value = {
    name: teamOverview.value?.teamName ?? '',
    defaultChatModelId: teamOverview.value?.defaultChatModelId ?? null,
    shareCode: teamOverview.value?.shareCode ?? '',
  }
  editTeamError.value = ''
  showEditTeamModal.value = true
  if (modelKeyList.value.length === 0) loadModelKeyList()
}

function createLocalShareCode() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '').slice(0, 12)
  }
  return Array.from({ length: 12 }, () => Math.floor(Math.random() * 16).toString(16)).join('')
}

function onGenerateShareCode() {
  if (editTeamForm.value.shareCode) {
    const ok = window.confirm('重新生成后，点击「确定」保存才会生效；取消关闭弹窗则不会写入。确定继续？')
    if (!ok) return
  }
  editTeamForm.value = {
    ...editTeamForm.value,
    shareCode: createLocalShareCode(),
  }
}

async function copyShareCode() {
  const code = editTeamForm.value.shareCode
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    toastSuccess('分享码已复制')
  } catch {
    toastError('复制失败，请手动复制')
  }
}

function closeEditTeamModal() {
  showEditTeamModal.value = false
}

async function submitEditTeam() {
  if (!editTeamForm.value.name.trim()) {
    editTeamError.value = '团队名称不能为空'
    return
  }
  editTeamSaving.value = true
  editTeamError.value = ''
  try {
    const shareCode = editTeamForm.value.shareCode.trim()
    const res = await updateTeam({
      name: editTeamForm.value.name.trim(),
      defaultChatModelId: editTeamForm.value.defaultChatModelId,
      ...(shareCode ? { shareCode } : {}),
    })
    if (res.success) {
      await loadTeamOverview()
      closeEditTeamModal()
      toastSuccess(shareCode ? '团队信息与分享码已保存' : '团队信息已更新')
    } else {
      editTeamError.value = res.message ?? '保存失败'
    }
  } catch (e: unknown) {
    editTeamError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    editTeamSaving.value = false
  }
}

function openEditAccountModal(account: TeamMember) {
  editingAccount.value = account
  editAccountForm.value = {
    nickname: account.nickname || '',
    phone: account.phone || '',
    status: account.status ?? MEMBER_STATUS.ENABLED,
    password: '',
  }
  editAccountError.value = ''
  showEditAccountModal.value = true
}

function closeEditAccountModal() {
  showEditAccountModal.value = false
  editingAccount.value = null
  editAccountError.value = ''
  editAccountForm.value.password = ''
}

function confirmDelete(account: TeamMember) {
  showConfirm(`确定要删除成员 "${account.username}" 吗？`, () => deleteMemberById(account.id))
}

function confirmDisableAccount(account: TeamMember) {
  showConfirm(`确定要禁用成员 "${account.username}" 吗？`, () => disableAccountById(account.id))
}

async function disableAccountById(id: number) {
  try {
    const res = await disableTeamMember(id)
    if (res.success) {
      toastSuccess('已禁用')
      loadTeamOverview()
    } else {
      toastError(res.message || '禁用失败')
    }
  } catch (e: any) {
    toastError(e.message || '禁用失败')
  }
}

async function enableAccount(account: TeamMember) {
  try {
    const res = await updateTeamMember(account.id, { status: MEMBER_STATUS.ENABLED })
    if (res.success) {
      toastSuccess('已启用')
      loadTeamOverview()
    } else {
      toastError(res.message || '启用失败')
    }
  } catch (e: any) {
    toastError(e.message || '启用失败')
  }
}

async function deleteMemberById(id: number) {
  try {
    const res = await deleteMember(id)
    if (res.success) {
      toastSuccess('已删除')
      loadTeamOverview()
    } else {
      toastError(res.message || '删除失败')
    }
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

async function submitAddAccount() {
  addAccountError.value = ''

  if (!addAccountForm.value.username || !addAccountForm.value.password) {
    addAccountError.value = '用户名和密码为必填项'
    return
  }

  addingAccount.value = true
  try {
    const res = await createTeamMember(addAccountForm.value)
    if (!res.success) {
      addAccountError.value = res.message || '添加失败'
      return
    }
    closeAddAccountModal()
    toastSuccess('成员添加成功')
    loadTeamOverview()
  } catch (e: any) {
    addAccountError.value = e.message || '添加失败'
  } finally {
    addingAccount.value = false
  }
}

async function submitEditAccount() {
  if (!editingAccount.value) return
  editAccountError.value = ''
  editingAccountSaving.value = true
  try {
    const res = await updateTeamMember(editingAccount.value.id, {
      nickname: editAccountForm.value.nickname || undefined,
      phone: editAccountForm.value.phone || undefined,
      status: editAccountForm.value.status,
      password: editAccountForm.value.password || undefined,
    })
    if (!res.success) {
      editAccountError.value = res.message || '保存失败'
      return
    }
    closeEditAccountModal()
    toastSuccess('保存成功')
    loadTeamOverview()
  } catch (e: any) {
    editAccountError.value = e.message || '保存失败'
  } finally {
    editingAccountSaving.value = false
  }
}

async function loadTeamOverview() {
  loadingMemberList.value = true
  try {
    const res = await getTeamOverview()
    if (res.success && res.data) {
      teamOverview.value = res.data
    }
  } catch (e: any) {
    toastError(e.message || '加载团队账号失败')
  } finally {
    loadingMemberList.value = false
  }
}

// API Key 管理
function formatDate(s: string | null): string {
  if (!s) return '-'
  try {
    const d = new Date(s)
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return s
  }
}

async function loadApiKeyList() {
  loadingApiKeyList.value = true
  try {
    const res = await getApiKeyList()
    if (res.success && Array.isArray(res.data)) {
      apiKeyList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingApiKeyList.value = false
  }
}

function closeCreateKeyModal() {
  showCreateKeyModal.value = false
  createKeyForm.value = { name: '', expiresInDays: undefined, chatAgentOptionId: null }
  createKeyError.value = ''
}

function closeNewKeyModal() {
  newKeyResult.value = null
  loadApiKeyList()
}

async function copyNewKey() {
  if (!newKeyResult.value?.apiKey) return
  try {
    await navigator.clipboard.writeText(newKeyResult.value.apiKey)
    alert('已复制到剪贴板')
  } catch {
    alert('复制失败，请手动复制')
  }
}

async function submitCreateKey() {
  createKeyError.value = ''
  creatingKey.value = true
  try {
    const params: { name?: string; expiresInDays?: number; chatAgentOptionId?: number | null } = {}
    if (createKeyForm.value.name.trim()) params.name = createKeyForm.value.name.trim()
    if (createKeyForm.value.expiresInDays != null && createKeyForm.value.expiresInDays > 0) {
      params.expiresInDays = createKeyForm.value.expiresInDays
    }
    if (createKeyForm.value.chatAgentOptionId != null) {
      params.chatAgentOptionId = createKeyForm.value.chatAgentOptionId
    }
    const res = await createApiKey(params)
    if (res.success && res.data) {
      closeCreateKeyModal()
      newKeyResult.value = res.data
    } else {
      createKeyError.value = res.message || '创建失败'
    }
  } catch (e: any) {
    createKeyError.value = e.message || '创建失败'
  } finally {
    creatingKey.value = false
  }
}


async function onKeyAgentChange(key: ApiKeyListItem, event: Event) {
  const raw = (event.target as HTMLSelectElement).value
  const nextId = raw === '' ? null : Number(raw)
  const previousId = key.chatAgentOptionId
  updatingKeyIds.value.add(key.id)
  try {
    const res = await updateApiKey(key.id, { chatAgentOptionId: nextId })
    if (res.success) {
      key.chatAgentOptionId = nextId
      key.chatAgentOptionName = nextId != null
        ? agentOptionList.value.find((a) => a.id === nextId)?.name ?? null
        : null
      toastSuccess('已更新绑定的 Agent')
    } else {
      toastError(res.message || '更新失败')
      key.chatAgentOptionId = previousId
      ;(event.target as HTMLSelectElement).value = previousId != null ? String(previousId) : ''
    }
  } catch (e: any) {
    toastError(e.message || '更新失败')
    key.chatAgentOptionId = previousId
    ;(event.target as HTMLSelectElement).value = previousId != null ? String(previousId) : ''
  } finally {
    updatingKeyIds.value.delete(key.id)
  }
}


function confirmDeleteKey(key: ApiKeyListItem) {
  showConfirm(`确定要删除 API Key「${key.name || key.apiKey}」吗？删除后不可恢复。`, () => deleteKeyById(key.id))
}

async function deleteKeyById(id: number) {
  try {
    await deleteApiKey(id)
    apiKeyList.value = apiKeyList.value.filter((k) => k.id !== id)
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

async function loadAgentOptionList() {
  loadingAgentOptionList.value = true
  try {
    const res = await getAgentOptionList()
    if (res.success && Array.isArray(res.data)) {
      agentOptionList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingAgentOptionList.value = false
  }
}

function confirmDeleteAgentOption(item: AgentOptionItem) {
  showConfirm(`确定删除 Agent 配置「${item.name}」吗？删除后不可恢复。`, () => deleteAgentOptionById(item.id))
}

async function deleteAgentOptionById(id: number) {
  try {
    const res = await deleteAgentOption(id)
    if (res.success) {
      agentOptionList.value = agentOptionList.value.filter((item) => item.id !== id)
      toastSuccess('已删除')
    } else {
      toastError(res.message || '删除失败')
    }
  } catch (e: unknown) {
    toastError(e instanceof Error ? e.message : '删除失败')
  }
}

async function loadBizConfigList() {
  loadingBizConfigList.value = true
  try {
    const res = await getBizConfigList()
    if (res.success && Array.isArray(res.data)) {
      bizConfigList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingBizConfigList.value = false
  }
}

function addBizConfigPair() {
  bizConfigPairs.value.push({ key: '', value: '' })
}

function removeBizConfigPair(index: number) {
  if (bizConfigPairs.value.length <= 1) return
  bizConfigPairs.value.splice(index, 1)
}

function buildBizConfigJsonFromPairs(): { ok: true; json: string } | { ok: false; message: string } {
  const obj: Record<string, string> = {}
  for (let i = 0; i < bizConfigPairs.value.length; i++) {
    const row = bizConfigPairs.value[i]
    if (!row) continue
    const k = row.key.trim()
    const v = row.value
    if (!k && (v === '' || v === undefined)) continue
    if (!k) {
      return { ok: false, message: `第 ${i + 1} 行：已填写子值但缺少子键名` }
    }
    if (Object.prototype.hasOwnProperty.call(obj, k)) {
      return { ok: false, message: `子键重复：${k}` }
    }
    obj[k] = v
  }
  if (Object.keys(obj).length === 0) {
    return { ok: false, message: '请至少添加一组有效的子键与子值（可保留一行并填完整）' }
  }
  return { ok: true, json: JSON.stringify(obj) }
}

/** 使用列表接口返回的 displayValue（仅键名 JSON）预填子键，子值留空 */
function bizConfigPairsFromListDisplay(displayValue: string): BizConfigPairRow[] {
  const fallback: BizConfigPairRow[] = [{ key: '', value: '' }]
  if (!displayValue || displayValue === '****') {
    return fallback
  }
  try {
    const parsed: unknown = JSON.parse(displayValue)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return fallback
    }
    const keys = Object.keys(parsed as Record<string, unknown>)
    if (keys.length === 0) {
      return fallback
    }
    return keys.map((k) => ({ key: k, value: '' }))
  } catch {
    return fallback
  }
}

/** 使用详情接口返回的 configValue（明文 JSON）预填子键和子值 */
function bizConfigPairsFromPlainValue(configValue: string): BizConfigPairRow[] {
  const fallback: BizConfigPairRow[] = [{ key: '', value: '' }]
  if (!configValue) return fallback
  try {
    const parsed: unknown = JSON.parse(configValue)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return fallback
    }
    const obj = parsed as Record<string, string>
    const keys = Object.keys(obj)
    if (keys.length === 0) return fallback
    return keys.map((k) => ({ key: k, value: obj[k] ?? '' }))
  } catch {
    return fallback
  }
}

async function openBizConfigModal(row: BizConfigItem | null) {
  bizConfigEditing.value = row
  bizConfigFormError.value = ''
  if (row) {
    // 先用列表数据预填基本信息
    bizConfigPairs.value = bizConfigPairsFromListDisplay(row.displayValue)
    bizConfigForm.value = {
      configKey: row.configKey,
      remark: row.remark || '',
    }
    showBizConfigModal.value = true
    // 异步加载明文配置值（预览模式下后端会返回 KEY 保留、VALUE 全遮掩的结果）
    loadingBizConfigDetail.value = true
    try {
      const res = await getBizConfigItem(row.configKey, row.memberId)
      if (res.success && res.data?.configValue) {
        bizConfigPairs.value = bizConfigPairsFromPlainValue(res.data.configValue)
      }
    } catch {
      // 加载失败时保持已有的键名预填
    } finally {
      loadingBizConfigDetail.value = false
    }
  } else {
    bizConfigPairs.value = [{ key: '', value: '' }]
    bizConfigForm.value = {
      configKey: '',
      remark: '',
    }
    showBizConfigModal.value = true
  }
}

function closeBizConfigModal() {
  showBizConfigModal.value = false
  bizConfigEditing.value = null
  bizConfigFormError.value = ''
  bizConfigPairs.value = [{ key: '', value: '' }]
}

async function submitBizConfig() {
  bizConfigFormError.value = ''
  const key = bizConfigForm.value.configKey.trim()
  if (!key) {
    bizConfigFormError.value = '请输入业务配置键'
    return
  }
  const built = buildBizConfigJsonFromPairs()
  if (built.ok === false) {
    bizConfigFormError.value = built.message
    return
  }
  const configValueJson = built.json
  submittingBizConfig.value = true
  try {
    const res = await saveBizConfigItem(key, {
      configValue: configValueJson,
      remark: bizConfigForm.value.remark.trim() || undefined,
    }, bizConfigEditing.value?.memberId)
    if (res.success) {
      toastSuccess('保存成功')
      closeBizConfigModal()
      await loadBizConfigList()
    } else {
      bizConfigFormError.value = res.message || '保存失败'
    }
  } catch (e: unknown) {
    bizConfigFormError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    submittingBizConfig.value = false
  }
}

function confirmDeleteBizConfig(row: BizConfigItem) {
  showConfirm(`确定删除外部服务配置「${row.configKey}」吗？删除后不可恢复。`, () => deleteBizConfigByKey(row.configKey, row.memberId))
}

async function deleteBizConfigByKey(configKey: string, memberId: number) {
  try {
    const res = await deleteBizConfigItem(configKey, memberId)
    if (res.success) {
      bizConfigList.value = bizConfigList.value.filter((r) => r.configKey !== configKey || r.memberId !== memberId)
      toastSuccess('已删除')
    }
  } catch (e: unknown) {
    toastError(e instanceof Error ? e.message : '删除失败')
  }
}

// 模型配置相关方法
async function loadModelKeyList() {
  loadingModelKeyList.value = true
  try {
    const res = await getModelKeyList()
    if (res.success && Array.isArray(res.data)) {
      modelKeyList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingModelKeyList.value = false
  }
}

function closeCreateModelModal() {
  showCreateModelModal.value = false
  createModelForm.value = {
    modelName: '',
    modelProvider: '',
    customProvider: '',
    keyType: 'chat',
    description: '',
    apiKey: '',
    visionSupported: false,
  }
  createModelError.value = ''
}

async function submitCreateModelKey() {
  createModelError.value = ''

  const provider =
    createModelForm.value.modelProvider === '自定义'
      ? createModelForm.value.customProvider.trim()
      : createModelForm.value.modelProvider

  if (!createModelForm.value.modelName.trim()) {
    createModelError.value = '请输入模型名称'
    return
  }
  if (!provider) {
    createModelError.value = '请选择或输入模型提供商'
    return
  }
  if (!createModelForm.value.keyType) {
    createModelError.value = '请选择模型类型'
    return
  }
  if (!createModelForm.value.apiKey.trim()) {
    createModelError.value = '请输入 API Key'
    return
  }

  creatingModelKey.value = true
  try {
    const capabilities: string[] = []
    if (isChatKeyType(createModelForm.value.keyType) && createModelForm.value.visionSupported) {
      capabilities.push('vision')
    }
    const res = await createModelKey({
      modelName: createModelForm.value.modelName.trim(),
      modelProvider: provider,
      keyType: createModelForm.value.keyType,
      apiKey: createModelForm.value.apiKey.trim(),
      description: createModelForm.value.description.trim() || undefined,
      capabilities: capabilities.length > 0 ? capabilities : undefined,
    })
    if (res.success) {
      closeCreateModelModal()
      loadModelKeyList()
    } else {
      createModelError.value = res.message || '创建失败'
    }
  } catch (e: any) {
    createModelError.value = e.message || '创建失败'
  } finally {
    creatingModelKey.value = false
  }
}

function confirmDeleteModelKey(item: ModelApiKeyItem) {
  showConfirm(`确定要删除模型「${item.modelName}」的 API Key 吗？删除后不可恢复。`, () => deleteModelKeyById(item.id))
}

async function deleteModelKeyById(id: number) {
  try {
    await deleteModelKey(id)
    modelKeyList.value = modelKeyList.value.filter((k) => k.id !== id)
    toastSuccess('删除成功')
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

async function toggleModelKeyEnabledStatus(item: ModelApiKeyItem) {
  try {
    const res = await toggleModelKeyEnabled(item.id)
    if (res.success && res.data) {
      const index = modelKeyList.value.findIndex((k) => k.id === item.id)
      if (index !== -1) {
        modelKeyList.value[index] = res.data
      }
      toastSuccess(res.data.enabled ? '模型已上线' : '模型已下线')
    } else {
      toastError(res.message || '操作失败')
    }
  } catch (e: any) {
    toastError(e.message || '操作失败')
  }
}

function isChatKeyType(keyType: string): boolean {
  return keyType === 'chat' || keyType === 'streaming_chat'
}

function openEditModelModal(item: ModelApiKeyItem) {
  editingModelKey.value = item
  editModelForm.value = {
    description: item.description || '',
    visionSupported: (item.capabilities || []).includes('vision'),
  }
  editModelError.value = ''
  showEditModelModal.value = true
}

function closeEditModelModal() {
  showEditModelModal.value = false
  editingModelKey.value = null
  editModelForm.value = { description: '', visionSupported: false }
  editModelError.value = ''
}

async function submitEditModelKey() {
  if (!editingModelKey.value) return
  editModelError.value = ''
  savingModelKey.value = true
  try {
    const capabilities: string[] = []
    if (isChatKeyType(editingModelKey.value.keyType) && editModelForm.value.visionSupported) {
      capabilities.push('vision')
    }
    const res = await updateModelKey(editingModelKey.value.id, {
      description: editModelForm.value.description.trim(),
      capabilities,
    })
    if (res.success && res.data) {
      const index = modelKeyList.value.findIndex((k) => k.id === editingModelKey.value!.id)
      if (index !== -1) modelKeyList.value[index] = res.data
      closeEditModelModal()
      toastSuccess('保存成功')
    } else {
      editModelError.value = res.message || '保存失败'
    }
  } catch (e: any) {
    editModelError.value = e.message || '保存失败'
  } finally {
    savingModelKey.value = false
  }
}

// Function Call 相关方法
async function goCreateFunctionCall() {
  try {
    await router.push({ name: 'functionCallCreate' })
  } catch (e: unknown) {
    toastError(e instanceof Error ? e.message : '无法打开新建工具页，请刷新后重试')
  }
}

async function loadFunctionCallList() {
  loadingFunctionCallList.value = true
  try {
    const res = await getFunctionCallList()
    if (res.success && Array.isArray(res.data)) {
      functionCallList.value = res.data
    }
  } catch {
    // 忽略
  } finally {
    loadingFunctionCallList.value = false
  }
}

function confirmDeleteFunctionCall(item: FunctionCallItem) {
  showConfirm(`确定要删除工具「${item.name}」吗？删除后不可恢复。`, () => deleteFunctionCallById(item.id))
}

async function deleteFunctionCallById(id: number) {
  try {
    await deleteFunctionCall(id)
    functionCallList.value = functionCallList.value.filter((k) => k.id !== id)
  } catch (e: any) {
    toastError(e.message || '删除失败')
  }
}

function keyTypeLabel(keyType: string): string {
  if (keyType === 'chat') return '对话'
  if (keyType === 'streaming_chat') return '流式对话'
  if (keyType === 'embedding') return '嵌入模型'
  if (keyType === 'moderation') return '审核'
  if (keyType === 'scoring') return '评分'
  if (keyType === 'image') return '图像'
  return keyType
}

function providerClass(provider: string): string {
  const p = provider.toLowerCase()
  if (p === 'openai') return 'provider-openai'
  if (p === 'anthropic') return 'provider-anthropic'
  if (p === 'deepseek') return 'provider-deepseek'
  if (p === 'ollama') return 'provider-ollama'
  if (p === 'qwen' || p.includes('qwen')) return 'provider-qwen'
  if (p === 'cohere') return 'provider-cohere'
  return 'provider-default'
}

function keyTypeClass(keyType: string): string {
  if (keyType === 'chat') return 'type-chat'
  if (keyType === 'streaming_chat') return 'type-streaming'
  if (keyType === 'embedding') return 'type-embedding'
  if (keyType === 'moderation') return 'type-moderation'
  if (keyType === 'scoring') return 'type-scoring'
  if (keyType === 'image') return 'type-image'
  return 'type-default'
}

function loadSectionData(section: string) {
  switch (section) {
    case 'documents': loadKnowledgeBaseList(); break
    case 'agentOption': loadAgentOptionList(); break
    case 'apikey': loadApiKeyList(); loadAgentOptionList(); break
    case 'bizConfig': loadBizConfigList(); break
    case 'modelConfig': loadModelKeyList(); break
    case 'functionCall': loadFunctionCallList(); break
    case 'accounts': loadTeamOverview(); break
  }
}

watch(currentSection, (section) => {
  loadSectionData(section)
})

watch(
  () => route.query.section,
  (section) => {
    if (typeof section === 'string' && menuItems.some((m) => m.id === section)) {
      currentSection.value = section
    }
  },
)

onMounted(() => {
  const section = route.query.section as string | undefined
  if (section && menuItems.some((m) => m.id === section)) {
    currentSection.value = section
  }
  loadSectionData(currentSection.value)
})


</script>

<style scoped>
.body-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100%;
  min-height: 0;
}

/* 页面顶部 header 条：标题描述左，操作按钮右 */
.section-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 2rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
  flex-shrink: 0;
}

.section-header-info {
  flex: 1;
  min-width: 0;
}

.section-header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
  flex-wrap: wrap;
}

/* 表格区主体 */
.section-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 1.5rem 2rem 2rem;
  min-height: 0;
}

/* 主操作按钮（上传/创建） */
.primary-action-btn {
  padding: 0.55rem 1.25rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #fff;
  background: var(--color-text-accent);
  border: 1px solid var(--color-text-accent);
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s ease;
  white-space: nowrap;
}

.primary-action-btn:hover:not(:disabled) {
  opacity: 0.88;
}

.primary-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.page-title {
  margin: 0 0 0.35rem;
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-desc {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

.user-info {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
}

.user-info strong {
  color: var(--color-text-primary);
}

.type-select {
  padding: 0.45rem 0.75rem;
  font-size: 0.875rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  min-width: 140px;
  cursor: pointer;
}

.type-select:hover,
.type-select:focus {
  border-color: var(--color-border-focus);
  outline: none;
}

.file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.data-table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.data-table-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
}

.data-table-card-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.team-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem;
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
}

.team-block-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: 0.01em;
}

.team-edit-btn {
  padding: 0.35rem 0.9rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-accent);
  background: transparent;
  border: 1px solid var(--color-text-accent);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  white-space: nowrap;
}

.team-edit-btn:hover {
  background: var(--color-text-accent);
  color: #fff;
}

.share-code-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.share-code-value {
  flex: 1;
  min-width: 8rem;
  padding: 0.55rem 0.75rem;
  font-size: 0.875rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  letter-spacing: 0.04em;
  user-select: all;
}

.form-hint {
  margin: 0.4rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
  line-height: 1.4;
}

.secondary-action-btn {
  padding: 0.45rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.secondary-action-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
}

.secondary-action-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.data-table-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 0.75rem;
}

.data-table-panel {
  position: relative;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--color-bg-card);
  flex: 1;
}

.data-table-panel.is-loading {
  min-height: 280px;
}

.data-table-panel > .upload-list,
.data-table-panel > .account-list,
.table-scroll-x > .upload-list,
.table-scroll-x > .account-list {
  margin-top: 0;
  border: none;
  border-radius: 0;
}

.table-scroll-x {
  overflow-x: auto;
  width: 100%;
}

.data-table-loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}

.data-table-loading-backdrop {
  position: absolute;
  inset: 0;
  background: var(--color-bg-card);
  opacity: 0.88;
  backdrop-filter: blur(4px);
}

.data-table-loading-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
}

.data-table-spinner {
  width: 44px;
  height: 44px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-text-accent);
  border-radius: 50%;
  animation: data-table-spin 0.7s linear infinite;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--color-text-accent) 25%, transparent);
}

@keyframes data-table-spin {
  to {
    transform: rotate(360deg);
  }
}

.data-table-loading-text {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.table-empty {
  margin: 0;
  padding: 4rem 1.5rem;
  text-align: center;
  font-size: 0.9375rem;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

.upload-list {
  margin-top: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-bg-card);
}

.upload-list .list-header {
  display: grid;
  grid-template-columns: 1fr 60px;
  align-items: center;
  padding: 0.75rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
  gap: 1rem;
}

/* 知识库列表 7 列布局 */
.kb-list .list-header {
  grid-template-columns: 1.2fr 1.5fr 80px 70px 100px 150px 150px;
  padding: 0.75rem 1.25rem;
}

.kb-list .upload-item {
  grid-template-columns: 1.2fr 1.5fr 80px 70px 100px 150px 150px;
  padding: 0.8rem 1.25rem;
}

.kb-list {
  min-width: 900px;
}

.kb-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-secondary);
}

/* 文档列表 9 列布局 */
.doc-list .list-header {
  grid-template-columns: 1.5fr 80px 100px 90px 70px 90px 150px 90px 130px;
  padding: 0.75rem 1.25rem;
}

.doc-list .upload-item {
  grid-template-columns: 1.5fr 80px 100px 90px 70px 90px 150px 90px 130px;
  padding: 0.8rem 1.25rem;
}

.doc-list {
  min-width: 1050px;
}

.doc-list .list-header span:last-child,
.doc-list .upload-item .upload-actions {
  text-align: center;
  justify-content: center;
}

/* 文档状态徽章 */
.doc-status-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  width: fit-content;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
}

.doc-status-done {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.doc-status-failed {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error, #ef4444);
}

.doc-status-pending {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.doc-status-processing {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.upload-list .list-header span:last-child {
  text-align: center;
}

.upload-item {
  display: grid;
  grid-template-columns: 1fr 60px;
  align-items: center;
  gap: 1rem;
  padding: 0.8rem 1.25rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.upload-item:hover {
  background: var(--color-bg-input);
}

.upload-item:last-child {
  border-bottom: none;
}

.upload-item.success {
  background: transparent;
}

.upload-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

.kb-name-link {
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  font: inherit;
  cursor: pointer;
  color: var(--color-primary, #7c3aed);
}

.kb-name-link:hover {
  text-decoration: underline;
}

.doc-file-type {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  font-family: ui-monospace, monospace;
}

.doc-doc-type {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.doc-file-size {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.doc-progress {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.doc-progress.progress-done {
  color: #22c55e;
  font-weight: 500;
}

.doc-progress .progress-complete {
  color: #22c55e;
  font-weight: 500;
}

.doc-chunks {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.doc-chunks.chunks-complete {
  color: #22c55e;
}

.doc-created-at {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.upload-status {
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.upload-actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* 账号管理样式 */
.add-account-section {
  display: none;
}

.refresh-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
  background: var(--color-bg-page);
}

.refresh-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.account-list {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-bg-card);
}

.list-header {
  padding: 0.75rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 0.02em;
}

.account-item {
  padding: 0.8rem 1.25rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.account-item:hover {
  background: var(--color-bg-input);
}

.account-item:last-child {
  border-bottom: none;
}

.account-info {
  display: contents;
}

.account-username {
  font-weight: 500;
  color: var(--color-text-primary);
}

.account-nickname {
  color: var(--color-text-secondary);
}

.account-phone {
  color: var(--color-text-tertiary);
}

.account-status {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
}

.account-status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.account-status.status-normal {
  background: rgba(25, 135, 84, 0.1);
  color: #198754;
}

.account-status.status-normal::before {
  background: #198754;
}

.account-status.status-disabled {
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.account-status.status-disabled::before {
  background: #6c757d;
}

.delete-btn {
  padding: 0.25rem 0.6rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.delete-btn:hover {
  color: var(--color-error);
  background: rgba(220, 53, 69, 0.08);
}

.current-user-tag {
  padding: 0.2rem 0.6rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
  border-radius: 999px;
  flex-shrink: 0;
}

/* 外部服务配置 - grid 布局 */
.biz-config-list .list-header,
.biz-config-list .account-item {
  display: grid;
  grid-template-columns: minmax(100px, 1.4fr) minmax(80px, 1.2fr) 100px 130px 100px;
  align-items: center;
  gap: 0.75rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.biz-config-list {
  min-width: 540px;
}

.biz-config-list .list-header span:last-child {
  text-align: center;
}

.biz-config-info {
  display: contents;
}

.biz-config-key {
  font-family: ui-monospace, monospace;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.biz-config-flag {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.biz-config-remark {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.biz-config-pair-list {
  margin-top: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--color-bg-page);
}

.biz-config-pair-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
}

.biz-config-pair-header span {
  flex: 1;
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.biz-config-pair-header-spacer {
  flex: 0 0 30px !important;
}

.biz-config-pair-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s ease;
}

.biz-config-pair-list .property-row:last-child {
  border-bottom: none;
}

.biz-config-pair-row .property-input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border-color: transparent;
  padding: 0.3rem 0.4rem;
  font-size: 0.875rem;
  border-radius: 6px;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.biz-config-pair-row .property-input:not(:disabled):hover {
  background: var(--color-bg-input);
  border-color: var(--color-border);
}

.biz-config-pair-row .property-input:focus {
  background: var(--color-bg-input);
  border-color: var(--color-border-focus);
}

.biz-config-pair-row .property-input:disabled {
  background: transparent;
  cursor: default;
}

.biz-config-pair-row .property-remove-btn {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  line-height: 1;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
}

.biz-config-pair-row .property-remove-btn:hover:not(:disabled) {
  color: var(--color-error);
  background: rgba(239, 68, 68, 0.08);
}

.biz-config-pair-row .property-remove-btn:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.property-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  width: 100%;
  padding: 0.55rem;
  font-size: 0.875rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-top: 1px dashed var(--color-border);
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
  border-radius: 0 0 10px 10px;
}

.property-add-btn:hover {
  color: var(--color-text-accent);
  background: rgba(99, 102, 241, 0.04);
}

.property-add-btn > span {
  font-size: 1rem;
  line-height: 1;
}

/* API Key 管理 - grid 布局 */
.apikey-list .list-header,
.apikey-list .account-item {
  display: grid;
  grid-template-columns: 1fr 1fr 1.2fr 1fr 60px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.apikey-list {
  min-width: 640px;
}

.apikey-list .list-header span:last-child {
  text-align: center;
}

.apikey-agent-select {
  width: 100%;
  max-width: 220px;
  padding: 0.35rem 0.5rem;
  font-size: 0.85rem;
}

.apikey-masked {
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.apikey-meta {
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
}

.key-display {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: var(--color-bg-input);
  border-radius: 8px;
  border: 1px solid var(--color-border-hover);
}

.key-value {
  flex: 1;
  font-size: 0.8125rem;
  word-break: break-all;
  color: var(--color-text-primary);
}

.copy-key-btn {
  padding: 0.35rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-hover);
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
}

.copy-key-btn:hover {
  border-color: var(--color-border-focus);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--color-bg-card);
  border-radius: 12px;
  width: 90%;
  max-width: 420px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.modal-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.modal-close {
  padding: 0.25rem 0.5rem;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-input);
}

.modal-body {
  padding: 1.25rem;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.35rem;
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.35rem;
}

.form-label-row .form-label {
  margin-bottom: 0;
}

.form-label-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-label .required {
  color: var(--color-error);
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
  background: var(--color-bg-input);
  border: 1px solid var(--color-border-hover);
  border-radius: 8px;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-border-focus);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-hint {
  margin: 0.35rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  line-height: 1.4;
}

.form-check {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  user-select: none;
}

.form-check input[type='checkbox'] {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
}

.form-error {
  margin: 0.5rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-error);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
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
  border: 1px solid var(--color-border-hover);
}

.btn-cancel:hover {
  border-color: var(--color-border-focus);
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

/* 账号管理 - grid 布局 */
.accounts-list .list-header,
.accounts-list .account-item {
  display: grid;
  grid-template-columns: 1fr 72px 1fr 1fr 80px 140px 160px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.accounts-list {
  min-width: 820px;
}

.accounts-list .list-header span:last-child {
  text-align: center;
}

.accounts-list .account-item .upload-actions {
  justify-self: center;
}

.accounts-list .account-item.is-current-user {
  background: rgba(13, 110, 253, 0.05);
}

.account-created {
  color: var(--color-text-tertiary);
  font-size: 0.8125rem;
}

.current-user-inline {
  margin-left: 0.4rem;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 600;
  color: #0d6efd;
  background: rgba(13, 110, 253, 0.12);
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  line-height: 1.2;
  justify-self: start;
}

.role-badge.role-owner {
  color: #0d6efd;
  background: rgba(13, 110, 253, 0.12);
}

.role-badge.role-member {
  color: #6f42c1;
  background: rgba(111, 66, 193, 0.12);
}

.role-badge.role-unknown {
  color: var(--color-text-tertiary);
  background: var(--color-bg-input);
}

/* 模型配置 - 表格布局 */
.model-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-bg-card);
  font-size: 0.875rem;
}

.model-table thead {
  background: var(--color-bg-input);
  border-bottom: 1px solid var(--color-border);
}

.model-table th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
  color: var(--color-text-tertiary);
  font-size: 0.8125rem;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.model-table tbody tr {
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.model-table tbody tr:last-child {
  border-bottom: none;
}

.model-table tbody tr:hover {
  background: rgba(0, 0, 0, 0.02);
}

.model-table td {
  padding: 0.75rem 1rem;
  vertical-align: top;
}

.col-model-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-family: ui-monospace, monospace;
  min-width: 120px;
}

.col-provider {
  min-width: 80px;
}

.col-type {
  min-width: 80px;
}

.col-description {
  min-width: 200px;
  max-width: 300px;
  word-break: break-word;
  white-space: normal;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.col-key {
  font-family: ui-monospace, monospace;
  min-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-status {
  min-width: 60px;
}

.col-time {
  min-width: 100px;
  white-space: nowrap;
}

.col-actions {
  min-width: 140px;
  display: flex;
  gap: 0.375rem;
}

.model-list .delete-btn,
.model-list .toggle-btn {
  padding: 0.25rem 0.6rem;
  font-size: 0.75rem;
  border: none;
}

.model-list .delete-btn:hover {
  background: rgba(220, 53, 69, 0.08);
  border-color: #dc3545;
}

.model-list .apikey-masked {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  font-size: 0.8125rem;
  font-family: ui-monospace, monospace;
}

.model-info {
  display: contents;
}

.model-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.model-provider-tag {
  justify-self: start;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.provider-openai { background: rgba(16, 163, 127, 0.1); color: #10a37f; }
.provider-anthropic { background: rgba(204, 120, 50, 0.1); color: #cc7832; }
.provider-deepseek { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.provider-ollama { background: rgba(108, 117, 125, 0.1); color: #6c757d; }
.provider-qwen { background: rgba(124, 58, 237, 0.1); color: #7c3aed; }
.provider-cohere { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

.model-type-tag {
  justify-self: start;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.type-chat { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.type-streaming { background: rgba(6, 182, 212, 0.1); color: #0891b2; }
.type-embedding { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
.type-moderation { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.type-scoring { background: rgba(217, 119, 6, 0.1); color: #d97706; }
.type-image { background: rgba(168, 85, 247, 0.1); color: #a855f7; }

.model-description {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  word-break: break-word;
  overflow-wrap: break-word;
  line-height: 1.4;
  max-height: 3.5em;
  overflow: hidden;
  min-width: 0;
  align-self: start;
}

.model-status-badge {
  display: inline-block;
  padding: 0;
  border-radius: 0;
  font-size: 0.8125rem;
  font-weight: 500;
  background: transparent;
  color: #ef4444;
  white-space: nowrap;
  text-align: center;
}

.model-status-badge.is-enabled {
  background: transparent;
  color: #16a34a;
}

.capability-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: var(--color-text-primary);
  white-space: nowrap;
  cursor: pointer;
}

.capability-toggle input[type='checkbox']:disabled + * {
  opacity: 0.6;
}

.toggle-btn {
  padding: 0.25rem 0.6rem;
  border-radius: 2px;
  font-size: 0.75rem;
  font-weight: 500;
  border: none;
  background: transparent;
  color: #16a34a;
  cursor: pointer;
  transition: color 0.15s ease;
  white-space: nowrap;
}

.toggle-btn.btn-enable {
  background: transparent;
  color: #16a34a;
}

.toggle-btn.btn-enable:hover {
  color: #15803d;
}

.toggle-btn.btn-disable {
  background: transparent;
  color: #ef4444;
}

.toggle-btn.btn-disable:hover {
  color: #dc2626;
}

.default-toggle-btn {
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  border: 1px solid transparent;
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease, border-color 0.15s ease, border-radius 0.15s ease;
  white-space: nowrap;
}

.default-toggle-btn .is-cancel {
  display: none;
}

.default-toggle-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
  border-color: rgba(239, 68, 68, 0.35);
  border-radius: 4px;
}

.default-toggle-btn:hover:not(:disabled) .is-default {
  display: none;
}

.default-toggle-btn:hover:not(:disabled) .is-cancel {
  display: inline;
}

.default-toggle-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.default-chat-badge {
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.set-default-btn {
  padding: 0.15rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #16a34a;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.35);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.set-default-btn:hover:not(:disabled) {
  color: #15803d;
  background: rgba(34, 197, 94, 0.18);
  border-color: rgba(34, 197, 94, 0.5);
}

.set-default-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.model-default-empty {
  color: var(--color-text-tertiary);
  font-size: 0.875rem;
}

/* 确认弹框 */
.modal-confirm {
  max-width: 360px;
}

.confirm-message {
  margin: 0;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.btn-danger {
  background: var(--color-error, #dc3545);
  border-color: var(--color-error, #dc3545);
}

.btn-danger:hover:not(:disabled) {
  opacity: 0.9;
}

/* Function Call - grid 布局 */
.fc-list .list-header,
.fc-list .account-item {
  display: grid;
  grid-template-columns: 150px 1fr 100px 140px 140px;
  align-items: center;
  gap: 1rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.fc-list {
  min-width: 540px;
}

.fc-list .list-header span:last-child {
  text-align: center;
}

.fc-info {
  display: contents;
}

.fc-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-family: ui-monospace, monospace;
  font-size: 0.875rem;
}

.fc-desc {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-btn {
  padding: 0.25rem 0.6rem;
  font-size: 0.8125rem;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.edit-btn:hover {
  color: var(--color-text-accent);
  background: rgba(59, 130, 246, 0.08);
}

.modal-wide {
  max-width: 620px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18), 0 2px 8px rgba(0, 0, 0, 0.06);
}

.form-textarea {
  resize: vertical;
  min-height: 2.5rem;
  font-family: inherit;
  line-height: 1.5;
}

/* Agent 配置 - grid 布局 */
.agent-option-list .list-header,
.agent-option-list .account-item {
  display: grid;
  grid-template-columns: minmax(120px, 1.2fr) 70px 60px 60px 90px 140px 120px;
  align-items: center;
  gap: 0.75rem;
  padding-left: 1.25rem;
  padding-right: 1.25rem;
}

.agent-option-list {
  min-width: 720px;
}

.agent-option-list .list-header span:last-child {
  text-align: center;
}

.agent-option-info {
  display: contents;
}

.agent-option-name {
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-option-flag {
  justify-self: start;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(108, 117, 125, 0.1);
  color: #6c757d;
}

.agent-option-flag.is-on {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

</style>
