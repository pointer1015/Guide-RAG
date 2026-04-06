<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores'
import { chatApi } from '@/api/modules'

const router = useRouter()
const sessionStore = useSessionStore()

// 状态控制
const activeMenuId = ref<string | null>(null)
const editingSessionId = ref<string | null>(null)
const editingTitle = ref('')
const renameInputRef = ref<HTMLInputElement | null>(null)

// 格式化时间
function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 切换会话
async function switchSession(sessionId: string) {
  if (sessionStore.currentSessionId === sessionId || editingSessionId.value === sessionId) return
  
  sessionStore.setCurrentSession(sessionId)
  router.push(`/chat/${sessionId}`)
  activeMenuId.value = null
}

// 菜单处理
function toggleMenu(sessionId: string, event: MouseEvent) {
  event.stopPropagation()
  if (activeMenuId.value === sessionId) {
    activeMenuId.value = null
  } else {
    activeMenuId.value = sessionId
  }
}

// 开始重命名
async function startRename(session: any, event: MouseEvent) {
  event.stopPropagation()
  activeMenuId.value = null
  editingSessionId.value = session.id
  editingTitle.value = session.title
  
  await nextTick()
  renameInputRef.value?.focus()
  renameInputRef.value?.select()
}

// 保存重命名
async function handleRename() {
  if (!editingSessionId.value) return
  
  const id = editingSessionId.value
  const newTitle = editingTitle.value.trim()
  
  if (newTitle && newTitle !== sessionStore.sessionList.find(s => s.id === id)?.title) {
    try {
      await chatApi.updateSession(id, { title: newTitle })
      sessionStore.updateSession(id, { title: newTitle })
    } catch (error) {
      console.error('重命名失败:', error)
    }
  }
  
  editingSessionId.value = null
}

// 取消重命名
function cancelRename() {
  editingSessionId.value = null
}

// 删除会话
async function deleteSession(sessionId: string, event: MouseEvent) {
  event.stopPropagation()
  activeMenuId.value = null
  
  try {
    await chatApi.deleteSession(sessionId)
    sessionStore.removeSession(sessionId)
    
    // 如果删除的是当前会话，切换到第一个
    if (sessionStore.currentSessionId === null && sessionStore.sessionList.length > 0) {
      const firstSessionId = sessionStore.sessionList[0].id
      sessionStore.setCurrentSession(firstSessionId)
      router.push(`/chat/${firstSessionId}`)
    } else if (sessionStore.sessionList.length === 0) {
      router.push('/chat')
    }
  } catch (error) {
    console.error('删除会话失败:', error)
  }
}

// 点击外部关闭菜单
const handleClickOutside = (_e: MouseEvent) => {
  if (activeMenuId.value) {
    activeMenuId.value = null
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

</script>

<template>
  <div class="session-sidebar">
    <div class="session-header">
      <span class="session-title">历史对话</span>
      <span class="session-count">{{ sessionStore.sessionList.length }}</span>
    </div>
    
    <div class="session-list">
      <div
        v-for="session in sessionStore.sortedSessions"
        :key="session.id"
        class="session-item"
        :class="{ active: session.isActive, editing: editingSessionId === session.id }"
        @click="switchSession(session.id)"
      >
        <div class="session-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        
        <div class="session-info">
          <template v-if="editingSessionId === session.id">
            <input
              ref="renameInputRef"
              v-model="editingTitle"
              class="rename-input"
              @keydown.enter="handleRename"
              @keydown.esc="cancelRename"
              @blur="handleRename"
              @click.stop
            />
          </template>
          <template v-else>
            <span class="session-name">{{ session.title }}</span>
            <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
          </template>
        </div>

        <div class="session-actions" v-if="editingSessionId !== session.id">
          <button
            class="session-more"
            @click="toggleMenu(session.id, $event)"
            title="更多操作"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <circle cx="12" cy="12" r="1" />
              <circle cx="12" cy="5" r="1" />
              <circle cx="12" cy="19" r="1" />
            </svg>
          </button>

          <Transition name="menu">
            <div v-if="activeMenuId === session.id" class="session-menu glass" @click.stop>
              <button class="menu-item" @click="startRename(session, $event)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                重命名
              </button>
              <button class="menu-item delete" @click="deleteSession(session.id, $event)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M10 11v6M14 11v6" />
                </svg>
                删除会话
              </button>
            </div>
          </Transition>
        </div>
      </div>
      
      <div v-if="sessionStore.sessionList.length === 0" class="session-empty">
        <p>暂无历史对话</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.session-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
}

.session-title {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.session-count {
  font-size: 0.75rem;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 0.125rem 0.5rem;
  border-radius: 100px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 0.5rem 0.5rem;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 0.25rem;
  position: relative; /* 为弹出菜单定位 */
}

.session-item:hover {
  background: var(--bg-hover);
}

.session-item.active {
  background: rgba(124, 58, 237, 0.1);
}

.session-item.editing {
  background: var(--bg-secondary);
  cursor: default;
}

.session-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.session-icon svg {
  width: 16px;
  height: 16px;
  color: var(--text-tertiary);
}

.session-item.active .session-icon svg {
  color: var(--brand-primary);
}

.session-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.session-name {
  font-size: 0.875rem;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-name {
  color: var(--text-primary);
}

.session-time {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

/* 编辑模式输入框 */
.rename-input {
  width: 100%;
  padding: 4px 8px;
  background: var(--bg-primary);
  border: 1px solid var(--brand-primary);
  border-radius: 4px;
  font-size: 0.875rem;
  color: var(--text-primary);
  outline: none;
}

/* 操作项 */
.session-actions {
  display: relative;
  flex-shrink: 0;
}

.session-more {
  display: none;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover .session-more,
.session-item.active .session-more {
  display: flex;
}

.session-more:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.session-more svg {
  width: 14px;
  height: 14px;
}

/* 弹出菜单 */
.session-menu {
  position: absolute;
  top: 100%;
  right: 0.5rem;
  min-width: 120px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  box-shadow: var(--shadow-lg);
  padding: 4px;
  z-index: 50;
  animation: slideIn 0.2s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-4px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.menu-item.delete:hover {
  background: var(--error-bg);
  color: var(--error);
}

.menu-item svg {
  width: 14px;
  height: 14px;
}

.session-empty {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.875rem;
}

/* 动画补丁 */
.menu-enter-active,
.menu-leave-active {
  transition: all 0.15s ease-out;
}

.menu-enter-from,
.menu-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.95);
}
</style>
