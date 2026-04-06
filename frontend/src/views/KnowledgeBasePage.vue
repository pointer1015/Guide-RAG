<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useKnowledgeBaseStore } from '@/stores'
import { useUpload, useDocumentPolling, useTheme } from '@/composables'
import { knowledgeBaseApi } from '@/api/modules'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'
import Input from '@/components/ui/Input.vue'
import Badge from '@/components/ui/Badge.vue'
import Progress from '@/components/ui/Progress.vue'

const kbStore = useKnowledgeBaseStore()
const { isDark } = useTheme()

// 当前选中的知识库
const selectedKbId = ref<string | null>(null)

// 上传处理
const { uploadFiles, isUploading } = useUpload(() => selectedKbId.value)

// 文档轮询
const { startPolling, hasProcessingDocuments } = useDocumentPolling(() => selectedKbId.value)

// 对话框状态
const showCreateDialog = ref(false)
const showDeleteDialog = ref(false)
const deleteTarget = ref<{ type: 'kb' | 'doc'; id: string; name: string } | null>(null)

// 创建知识库表单
const createForm = ref({
  name: '',
  description: ''
})
const createLoading = ref(false)

// 加载知识库列表
async function loadKnowledgeBases() {
  kbStore.setLoading(true)
  try {
    const response = await knowledgeBaseApi.getKnowledgeBases()
    // 映射后端响应格式到前端格式
    const kbList = (response.data.list || []).map(kb => ({
      id: kb.id,
      name: kb.name,
      description: kb.description,
      documentCount: kb.docCount || 0,
      createdAt: kb.gmtCreate,
      updatedAt: kb.gmtModified || kb.gmtCreate
    }))
    kbStore.setKnowledgeBases(kbList)
    // 默认选中第一个
    if (kbList.length > 0 && !selectedKbId.value) {
      selectKb(kbList[0].id)
    }
  } finally {
    kbStore.setLoading(false)
  }
}

// 选择知识库
async function selectKb(id: string) {
  selectedKbId.value = id
  kbStore.setCurrentKb(id)
  
  // 加载文档列表
  kbStore.setDocumentsLoading(true)
  try {
    const response = await knowledgeBaseApi.getDocuments(id)
    // 映射后端响应格式到前端格式
    // 注意：后端返回 PageInfo 结构，列表字段为 list（不是 items）
    // 文档字段：id（不是 docId）、fileSize（不是 size）、gmtCreate（不是 uploadedAt）
    const rawList = response.data.list || []
    const docList = rawList.map((d: any) => ({
      id: String(d.id),
      name: d.title || d.fileName || 'Unknown',
      size: d.fileSize || 0,
      type: d.mimeType || '',
      status: d.parseStatus,
      progress: d.progress || 0,
      errorMessage: d.errorMessage || undefined,
      createdAt: d.gmtCreate || ''
    }))
    kbStore.setDocuments(docList)
    
    // 如果有处理中的文档，启动轮询
    if (hasProcessingDocuments()) {
      startPolling()
    }
  } finally {
    kbStore.setDocumentsLoading(false)
  }
}

// 创建知识库
async function handleCreate() {
  if (!createForm.value.name.trim()) return
  
  createLoading.value = true
  try {
    const response = await knowledgeBaseApi.createKnowledgeBase({
      name: createForm.value.name,
      description: createForm.value.description || undefined
    })
    // 映射后端响应格式
    const newKb = {
      id: response.data.id,
      name: response.data.name,
      description: response.data.description,
      documentCount: response.data.docCount || 0,
      createdAt: response.data.gmtCreate,
      updatedAt: response.data.gmtModified || response.data.gmtCreate
    }
    kbStore.addKnowledgeBase(newKb)
    showCreateDialog.value = false
    createForm.value = { name: '', description: '' }
    selectKb(newKb.id)
  } finally {
    createLoading.value = false
  }
}

// 删除确认
function confirmDelete(type: 'kb' | 'doc', id: string, name: string) {
  deleteTarget.value = { type, id, name }
  showDeleteDialog.value = true
}

// 执行删除
async function handleDelete() {
  if (!deleteTarget.value) return
  
  const { type, id } = deleteTarget.value
  
  if (type === 'kb') {
    await knowledgeBaseApi.deleteKnowledgeBase(id)
    kbStore.removeKnowledgeBase(id)
    if (selectedKbId.value === id) {
      selectedKbId.value = kbStore.knowledgeBases[0]?.id || null
      if (selectedKbId.value) {
        selectKb(selectedKbId.value)
      }
    }
  } else {
    await knowledgeBaseApi.deleteDocument(selectedKbId.value!, id)
    kbStore.removeDocument(id)
  }
  
  showDeleteDialog.value = false
  deleteTarget.value = null
}

// 文件上传
const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    await uploadFiles(target.files)
    target.value = '' // 重置 input
    
    // 启动轮询
    startPolling()
  }
}

// 重新处理文档
async function reprocessDocument(docId: string) {
  if (!selectedKbId.value) return
  await knowledgeBaseApi.reprocessDocument(selectedKbId.value, docId)
  kbStore.updateDocument(docId, { status: 'PENDING', progress: 0, errorMessage: undefined })
  startPolling()
}

// 格式化文件大小
function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 获取状态徽章
function getStatusBadge(status: string) {
  const map: Record<string, { variant: 'success' | 'warning' | 'error' | 'info'; label: string }> = {
    'PARSED': { variant: 'success', label: '已完成' },
    'PARSING': { variant: 'info', label: '解析中' },
    'PENDING': { variant: 'warning', label: '等待中' },
    'FAILED': { variant: 'error', label: '失败' },
    'uploading': { variant: 'info', label: '上传中' }
  }
  return map[status] || { variant: 'info', label: status }
}

onMounted(() => {
  loadKnowledgeBases()
})

// 拖拽上传
const isDragging = ref(false)

async function handleDrop(event: DragEvent) {
  isDragging.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0 && selectedKbId.value) {
    await uploadFiles(files)
    startPolling()
  }
}

// 格式化日期
function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="kb-page" :class="{ 'is-dark': isDark }">
    <!-- 左侧：知识库列表 -->
    <aside class="kb-list">
      <div class="kb-list-header">
        <h2 class="kb-list-title">知识库</h2>
        <Button size="sm" @click="showCreateDialog = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 16px; height: 16px;">
            <path d="M12 5v14m-7-7h14"/>
          </svg>
          新建
        </Button>
      </div>
      
      <div class="kb-items">
        <TransitionGroup name="kb-list">
          <div
            v-for="kb in kbStore.knowledgeBases"
            :key="kb.id"
            class="kb-item"
            :class="{ active: selectedKbId === kb.id }"
            @click="selectKb(kb.id)"
          >
            <div class="kb-item-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <ellipse cx="12" cy="5" rx="9" ry="3"/>
                <path d="M3 5V19A9 3 0 0 0 21 19V5"/>
                <path d="M3 12A9 3 0 0 0 21 12"/>
              </svg>
            </div>
            <div class="kb-item-info">
              <span class="kb-item-name">{{ kb.name }}</span>
              <span class="kb-item-count">{{ kb.documentCount }} 个文档</span>
            </div>
            <button
              class="kb-item-delete"
              @click.stop="confirmDelete('kb', kb.id, kb.name)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </TransitionGroup>
        
        <!-- 空状态 -->
        <div v-if="kbStore.knowledgeBases.length === 0 && !kbStore.loading" class="kb-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <ellipse cx="12" cy="5" rx="9" ry="3"/>
            <path d="M3 5V19A9 3 0 0 0 21 19V5"/>
            <path d="M3 12A9 3 0 0 0 21 12"/>
          </svg>
          <p>暂无知识库</p>
          <Button size="sm" @click="showCreateDialog = true">创建第一个</Button>
        </div>
      </div>
    </aside>

    <!-- 右侧：文档列表 -->
    <main class="doc-panel">
      <template v-if="selectedKbId">
        <div class="doc-header glass">
          <div class="doc-header-info">
            <h2 class="doc-title">{{ kbStore.currentKb?.name }}</h2>
            <p class="doc-desc">{{ kbStore.currentKb?.description || '暂无描述' }}</p>
          </div>
          <div class="doc-header-actions">
            <div class="doc-stats">
              <div class="stat">
                <span class="stat-value">{{ kbStore.documents.length }}</span>
                <span class="stat-label">文档</span>
              </div>
              <div class="stat">
                <span class="stat-value">{{ kbStore.documents.filter(d => d.status === 'PARSED').length }}</span>
                <span class="stat-label">已解析</span>
              </div>
            </div>
            <Button @click="triggerUpload" :loading="isUploading">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width: 18px; height: 18px;">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              上传文档
            </Button>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            multiple
            accept=".pdf,.doc,.docx,.md,.txt"
            style="display: none;"
            @change="handleFileChange"
          />
        </div>

        <!-- 拖拽上传区 -->
        <div 
          class="drop-zone"
          :class="{ 'is-dragging': isDragging }"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="handleDrop"
        >
          <div v-if="isDragging" class="drop-overlay">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <span>释放文件以上传</span>
          </div>

          <!-- 文档列表 -->
          <div v-if="kbStore.documentsLoading" class="doc-loading">
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <span>加载文档中...</span>
          </div>
          
          <div v-else-if="kbStore.documents.length === 0" class="doc-empty">
            <div class="empty-visual">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="12" y1="18" x2="12" y2="12"/>
                <line x1="9" y1="15" x2="15" y2="15"/>
              </svg>
            </div>
            <h3>暂无文档</h3>
            <p>拖拽文件到此处或点击上方按钮上传</p>
            <p class="supported-formats">支持 PDF、Word、Markdown、TXT 格式</p>
          </div>

          <div v-else class="doc-list">
            <TransitionGroup name="doc-list">
              <div
                v-for="doc in kbStore.sortedDocuments"
                :key="doc.id"
                class="doc-item"
                :class="[`status-${doc.status.toLowerCase()}`]"
              >
                <div class="doc-item-icon">
                  <svg v-if="doc.type === 'application/pdf'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <path d="M10 12h4"/>
                    <path d="M10 16h4"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                </div>
                
                <div class="doc-item-info">
                  <span class="doc-item-name">{{ doc.name }}</span>
                  <span class="doc-item-meta">
                    {{ formatSize(doc.size) }}
                    <span class="meta-dot">·</span>
                    {{ formatDate(doc.createdAt) }}
                  </span>
                </div>

                <div class="doc-item-status">
                  <Badge :variant="getStatusBadge(doc.status).variant" dot>
                    {{ getStatusBadge(doc.status).label }}
                  </Badge>
                  
                  <Progress
                    v-if="doc.status === 'PARSING' || doc.status === 'uploading'"
                    :value="doc.progress"
                    size="sm"
                    style="width: 80px;"
                  />
                  
                  <span v-if="doc.status === 'FAILED'" class="error-msg" :title="doc.errorMessage">
                    {{ doc.errorMessage }}
                  </span>
                </div>

                <div class="doc-item-actions">
                  <button
                    v-if="doc.status === 'FAILED'"
                    class="action-btn"
                    title="重新处理"
                    @click="reprocessDocument(doc.id)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="23 4 23 10 17 10"/>
                      <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                    </svg>
                  </button>
                  <button
                    class="action-btn danger"
                    title="删除"
                    @click="confirmDelete('doc', doc.id, doc.name)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    </svg>
                  </button>
                </div>
              </div>
            </TransitionGroup>
          </div>
        </div>
      </template>

      <div v-else class="no-kb-selected">
        <div class="no-kb-visual">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <ellipse cx="12" cy="5" rx="9" ry="3"/>
            <path d="M3 5V19A9 3 0 0 0 21 19V5"/>
            <path d="M3 12A9 3 0 0 0 21 12"/>
          </svg>
        </div>
        <h3>选择知识库</h3>
        <p>从左侧选择一个知识库开始管理文档</p>
      </div>
    </main>

    <!-- 创建知识库对话框 -->
    <Dialog v-model="showCreateDialog" title="新建知识库">
      <div class="form-group">
        <label class="form-label">名称</label>
        <Input v-model="createForm.name" placeholder="请输入知识库名称" />
      </div>
      <div class="form-group">
        <label class="form-label">描述（可选）</label>
        <textarea
          v-model="createForm.description"
          class="form-textarea"
          placeholder="请输入知识库描述"
          rows="3"
        />
      </div>
      <template #footer>
        <Button variant="secondary" @click="showCreateDialog = false">取消</Button>
        <Button :loading="createLoading" @click="handleCreate">创建</Button>
      </template>
    </Dialog>

    <!-- 删除确认对话框 -->
    <Dialog v-model="showDeleteDialog" title="确认删除">
      <p class="delete-confirm-text">
        确定要删除{{ deleteTarget?.type === 'kb' ? '知识库' : '文档' }}
        <strong>「{{ deleteTarget?.name }}」</strong> 吗？
        <br />
        <span v-if="deleteTarget?.type === 'kb'" class="delete-warning">
          此操作将同时删除该知识库下的所有文档，且无法恢复。
        </span>
      </p>
      <template #footer>
        <Button variant="secondary" @click="showDeleteDialog = false">取消</Button>
        <Button variant="danger" @click="handleDelete">确认删除</Button>
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.kb-page {
  height: 100%;
  display: flex;
  background: var(--bg-primary);
}

/* 知识库列表 */
.kb-list {
  width: 300px;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-primary);
}

.kb-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-primary);
}

.kb-list-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.kb-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.kb-item:hover {
  background: var(--bg-hover);
}

.kb-item.active {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1), rgba(59, 130, 246, 0.1));
  border: 1px solid rgba(124, 58, 237, 0.2);
}

.kb-item-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-gradient);
  border-radius: 10px;
  flex-shrink: 0;
}

.kb-item-icon svg {
  width: 20px;
  height: 20px;
  color: white;
}

.kb-item-info {
  flex: 1;
  min-width: 0;
}

.kb-item-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-item-count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.kb-item-delete {
  opacity: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.kb-item:hover .kb-item-delete {
  opacity: 1;
}

.kb-item-delete:hover {
  background: var(--error-bg);
  color: var(--error);
}

.kb-item-delete svg {
  width: 16px;
  height: 16px;
}

/* 空状态 */
.kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  color: var(--text-tertiary);
}

.kb-empty svg {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.kb-empty p {
  margin: 0 0 16px;
}

/* 文档面板 */
.doc-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.doc-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 24px;
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-primary);
}

.doc-header-info {
  flex: 1;
}

.doc-header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.doc-stats {
  display: flex;
  gap: 20px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.doc-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.doc-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 拖拽上传区 */
.drop-zone {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.drop-zone.is-dragging {
  background: rgba(124, 58, 237, 0.05);
}

.drop-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(124, 58, 237, 0.1);
  border: 2px dashed var(--brand-primary);
  border-radius: 16px;
  margin: 16px;
  z-index: 10;
  color: var(--brand-primary);
  font-weight: 500;
}

.drop-overlay svg {
  width: 48px;
  height: 48px;
}

/* 加载和空状态 */
.doc-loading,
.doc-empty,
.no-kb-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-tertiary);
  padding: 40px;
}

.loading-dots {
  display: flex;
  gap: 6px;
}

.loading-dots span {
  width: 10px;
  height: 10px;
  background: var(--brand-primary);
  border-radius: 50%;
  animation: loadingDot 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes loadingDot {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.empty-visual,
.no-kb-visual {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1), rgba(59, 130, 246, 0.1));
  border-radius: 20px;
  margin-bottom: 8px;
}

.empty-visual svg,
.no-kb-visual svg {
  width: 36px;
  height: 36px;
  color: var(--brand-primary);
}

.doc-empty h3,
.no-kb-selected h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.doc-empty p,
.no-kb-selected p {
  margin: 0;
  color: var(--text-secondary);
}

.supported-formats {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
}

/* 文档列表 */
.doc-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  margin-bottom: 12px;
  transition: all 0.2s ease;
}

.doc-item:hover {
  border-color: var(--border-secondary);
  transform: translateX(4px);
}

.doc-item.status-parsed {
  border-left: 3px solid var(--success);
}

.doc-item.status-parsing {
  border-left: 3px solid var(--info);
}

.doc-item.status-pending {
  border-left: 3px solid var(--warning);
}

.doc-item.status-failed {
  border-left: 3px solid var(--error);
}

.doc-item-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: 10px;
  flex-shrink: 0;
}

.doc-item-icon svg {
  width: 22px;
  height: 22px;
  color: var(--text-tertiary);
}

.doc-item-info {
  flex: 1;
  min-width: 0;
}

.doc-item-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-item-meta {
  font-size: 12px;
  color: var(--text-tertiary);
}

.meta-dot {
  margin: 0 6px;
}

.doc-item-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.error-msg {
  max-width: 120px;
  font-size: 12px;
  color: var(--error);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-item-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
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

.action-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.action-btn.danger:hover {
  background: var(--error-bg);
  color: var(--error);
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

/* Glass */
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--glass-border);
}

/* 表单 */
.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 14px;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

.form-textarea:focus {
  border-color: var(--brand-primary);
}

.delete-confirm-text {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.delete-confirm-text strong {
  color: var(--text-primary);
}

.delete-warning {
  color: var(--error);
  font-size: 13px;
}

/* 动画 */
.kb-list-enter-active,
.kb-list-leave-active,
.doc-list-enter-active,
.doc-list-leave-active {
  transition: all 0.3s ease;
}

.kb-list-enter-from,
.doc-list-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.kb-list-leave-to,
.doc-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .kb-page {
    flex-direction: column;
  }
  
  .kb-list {
    width: 100%;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid var(--border-primary);
  }
  
  .kb-items {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding: 8px;
  }
  
  .kb-item {
    flex-shrink: 0;
    min-width: 150px;
  }
  
  .doc-header {
    flex-wrap: wrap;
  }
  
  .doc-stats {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    margin-top: 12px;
  }
}
</style>
