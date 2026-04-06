<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useModelConfigStore, type ModelOverrideConfig, MODEL_PRESETS } from '@/stores/modelConfig'

const modelConfigStore = useModelConfigStore()

// 弹窗可见性
const showModal = ref(false)

interface Props {
  isCollapsed?: boolean
}
defineProps<Props>()

// 保存中/加载中状态
const saving = ref(false)
const errorMsg = ref('')

// 表单数据
const formProvider = ref('openai')
const formApiKey = ref('')
const formBaseUrl = ref('')
const formModel = ref('')
const showApiKey = ref(false)

// 当前选中的预设
const selectedPreset = computed(() =>
  MODEL_PRESETS.find(p => p.id === formProvider.value)
)

// 监听 Provider 切换，自动填入默认 Base URL
watch(formProvider, (newProvider) => {
  const preset = MODEL_PRESETS.find(p => p.id === newProvider)
  if (preset && preset.defaultBaseUrl) {
    formBaseUrl.value = preset.defaultBaseUrl
  }
})

// 表单是否有效
const isFormValid = computed(() => {
  return formBaseUrl.value.trim() !== '' && formModel.value.trim() !== ''
})

// 打开弹窗时，加载当前配置
async function openModal() {
  errorMsg.value = ''
  
  // 优先显示本地缓存（用户体验：立即响应）
  if (modelConfigStore.localConfig) {
    formProvider.value = modelConfigStore.localConfig.provider || 'openai'
    formApiKey.value = modelConfigStore.localConfig.apiKey || ''
    formBaseUrl.value = modelConfigStore.localConfig.baseUrl || ''
    formModel.value = modelConfigStore.localConfig.model || ''
  }

  showApiKey.value = false
  showModal.value = true

  // 异步拉取最新配置（静默更新或填充）
  if (!modelConfigStore.loading) {
    await modelConfigStore.fetchConfig()
    
    // 如果拉取成功且本地没配置，或者本地配置已过期，则同步到表单
    if (modelConfigStore.serverConfig) {
      formProvider.value = modelConfigStore.serverConfig.provider || 'openai'
      // API Key 保持现状（除非用户手动重新输入，否则使用本地缓存或留空）
      formBaseUrl.value = modelConfigStore.serverConfig.baseUrl || ''
      formModel.value = modelConfigStore.serverConfig.model || ''
    }
  }
}

// 保存配置到后端
async function saveConfig() {
  if (!isFormValid.value || saving.value) return
  errorMsg.value = ''
  saving.value = true

  try {
    const config: ModelOverrideConfig = {
      provider: formProvider.value,
      apiKey: formApiKey.value,
      baseUrl: formBaseUrl.value.trim(),
      model: formModel.value.trim()
    }
    await modelConfigStore.saveConfig(config)
    showModal.value = false
  } catch (e: any) {
    errorMsg.value = e?.message || '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

// 重置为默认
async function resetToDefault() {
  errorMsg.value = ''
  saving.value = true
  try {
    await modelConfigStore.deleteConfig()
    showModal.value = false
  } catch (e: any) {
    errorMsg.value = e?.message || '操作失败，请重试'
  } finally {
    saving.value = false
  }
}

// 遮罩层点击关闭
function closeModal() {
  showModal.value = false
}
</script>

<template>
  <!-- 触发按钮 -->
  <button class="model-trigger" :class="{ active: modelConfigStore.isCustomModel, collapsed: isCollapsed }" @click="openModal" :title="modelConfigStore.displayName">
    <span class="model-icon">
      <svg v-if="modelConfigStore.displayIconData.svgPath" :viewBox="modelConfigStore.displayIconData.viewBox || '0 0 24 24'" fill="currentColor">
        <path :d="modelConfigStore.displayIconData.svgPath" />
      </svg>
      <template v-else>{{ modelConfigStore.displayIconData.icon }}</template>
    </span>
    <span v-if="!isCollapsed" class="model-name">{{ modelConfigStore.displayName }}</span>
    <svg v-if="!isCollapsed" class="trigger-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  </button>

  <!-- 弹窗遮罩 -->
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <Transition name="modal-slide">
          <div v-if="showModal" class="modal-content glass">
            <!-- 弹窗头部 -->
            <div class="modal-header">
              <h3 class="modal-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="3" />
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
                </svg>
                模型配置
              </h3>
              <button class="close-btn" @click="closeModal">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <div v-if="modelConfigStore.loading && !modelConfigStore.serverConfig" class="modal-loading">
              <span class="loading-spinner"></span>
              <span>正在获取云端配置...</span>
            </div>

            <template v-else>
              <!-- 提供商选择 -->
            <div class="form-section">
              <label class="form-label">模型提供商</label>
              <div class="provider-grid">
                <button
                  v-for="preset in MODEL_PRESETS"
                  :key="preset.id"
                  class="provider-card"
                  :class="{ selected: formProvider === preset.id }"
                  @click="formProvider = preset.id"
                >
                  <span class="provider-icon">
                    <svg v-if="preset.svgPath" :viewBox="preset.viewBox || '0 0 24 24'" fill="currentColor">
                      <path :d="preset.svgPath" />
                    </svg>
                    <template v-else>{{ preset.icon }}</template>
                  </span>
                  <span class="provider-name">{{ preset.name }}</span>
                </button>
              </div>
            </div>

            <!-- API Key -->
            <div class="form-section">
              <label class="form-label">
                API Key
                <span v-if="formProvider === 'ollama'" class="label-hint">（本地模型无需填写）</span>
              </label>
              <div class="input-wrapper">
                <input
                  :type="showApiKey ? 'text' : 'password'"
                  v-model="formApiKey"
                  class="form-input"
                  placeholder="sk-xxxxxxxxxxxx"
                  autocomplete="off"
                />
                <button class="input-addon" @click="showApiKey = !showApiKey" type="button">
                  <svg v-if="showApiKey" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                    <line x1="1" y1="1" x2="23" y2="23" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- Base URL -->
            <div class="form-section">
              <label class="form-label">Base URL</label>
              <input
                type="text"
                v-model="formBaseUrl"
                class="form-input"
                placeholder="https://api.example.com/v1"
              />
            </div>

            <!-- Model -->
            <div class="form-section">
              <label class="form-label">模型名称</label>
              <input
                type="text"
                v-model="formModel"
                class="form-input"
                :placeholder="selectedPreset?.placeholder || '输入模型名称'"
              />
            </div>

            <!-- 安全提示 -->
            <div class="security-notice">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
              <span>模型配置将保存至您的账号，API Key 在传输中加密处理</span>
            </div>

            <!-- 错误信息 -->
            <div v-if="errorMsg" class="error-notice">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="15" y1="9" x2="9" y2="15" />
                <line x1="9" y1="9" x2="15" y2="15" />
              </svg>
              <span>{{ errorMsg }}</span>
            </div>

            <!-- 操作按钮 -->
            <div class="modal-actions">
              <button
                v-if="modelConfigStore.hasServerConfig"
                class="action-btn reset-btn"
                :disabled="saving"
                @click="resetToDefault"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="1 4 1 10 7 10" />
                  <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                </svg>
                恢复默认
              </button>
              <div class="action-spacer"></div>
              <button class="action-btn cancel-btn" @click="closeModal">取消</button>
              <button
                class="action-btn save-btn"
                :disabled="!isFormValid || saving"
                @click="saveConfig"
              >
                <template v-if="saving">
                  <span class="btn-spinner"></span>
                  保存中...
                </template>
                <template v-else>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  保存配置
                </template>
              </button>
            </div>
          </template>
        </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 触发按钮 */
.model-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0.625rem 0.75rem;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  overflow: hidden;
}

.model-trigger.collapsed {
  justify-content: center;
  padding: 0.625rem 0.75rem;
}

.model-trigger:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.model-trigger.active {
  color: var(--brand-primary);
}

.model-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.model-icon svg {
  width: 100%;
  height: 100%;
}

.model-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trigger-chevron {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* 弹窗遮罩 */
.modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 9999;
}

/* 弹窗内容 */
.modal-content {
  width: 520px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
  padding: 28px;
  background: var(--bg-elevated, #1a1a2e);
  border: 1px solid var(--border-primary);
  border-radius: 20px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
}

/* 头部 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.modal-title svg {
  width: 22px;
  height: 22px;
  color: var(--brand-primary);
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.close-btn svg {
  width: 18px;
  height: 18px;
}

/* 表单区域 */
.form-section {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.label-hint {
  font-weight: 400;
  color: var(--text-tertiary);
  font-size: 12px;
}

/* 提供商选择网格 */
.provider-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.provider-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  background: var(--bg-secondary);
  border: 1.5px solid var(--border-primary);
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.provider-card:hover {
  background: var(--bg-hover);
  border-color: var(--border-secondary);
  color: var(--text-primary);
  transform: translateY(-1px);
}

.provider-card.selected {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.12), rgba(59, 130, 246, 0.12));
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  box-shadow: 0 0 0 1px rgba(124, 58, 237, 0.2);
}

.provider-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-bottom: 4px;
}

.provider-icon svg {
  width: 100%;
  height: 100%;
}

.provider-name {
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
}

/* 输入框 */
.form-input {
  width: 100%;
  padding: 10px 14px;
  background: var(--bg-secondary);
  border: 1.5px solid var(--border-primary);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  transition: all 0.2s;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.form-input::placeholder {
  color: var(--text-tertiary);
  font-family: inherit;
}

/* 密码输入包装器 */
.input-wrapper {
  position: relative;
  display: flex;
}

.input-wrapper .form-input {
  padding-right: 44px;
}

.input-addon {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.input-addon:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.input-addon svg {
  width: 18px;
  height: 18px;
}

/* 安全提示 */
.security-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: 10px;
  font-size: 12px;
  color: #22c55e;
  margin-bottom: 16px;
}

.security-notice svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* 错误提示 */
.error-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 10px;
  font-size: 12px;
  color: #ef4444;
  margin-bottom: 16px;
}

.error-notice svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* 操作按钮 */
.modal-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-spacer {
  flex: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reset-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.reset-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
}

.cancel-btn {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  border: 1px solid var(--border-primary);
}

.cancel-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.save-btn {
  background: var(--brand-gradient);
  color: white;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.4);
}

/* 保存按钮 spinner */
.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 弹窗动画 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-slide-leave-active {
  transition: all 0.2s ease;
}

.modal-slide-enter-from {
  opacity: 0;
  transform: scale(0.92) translateY(20px);
}

.modal-slide-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(10px);
}

/* 响应式 */
@media (max-width: 560px) {
  .provider-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .modal-content {
    padding: 20px;
  }

  .modal-actions {
    flex-wrap: wrap;
  }
}

/* 加载中状态 */
.modal-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 16px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--bg-tertiary);
  border-top-color: var(--brand-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
</style>
