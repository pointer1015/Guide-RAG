import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { SessionItem } from '@/types'

export const useSessionStore = defineStore('session', () => {
  // State
  const currentSessionId = ref<string | null>(null)
  const sessionList = ref<SessionItem[]>([])
  const loading = ref(false)

  // Getters
  const currentSession = computed(() => 
    sessionList.value.find(s => s.id === currentSessionId.value) || null
  )

  const sortedSessions = computed(() => 
    [...sessionList.value].sort((a, b) => 
      new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    )
  )

  // Actions
  function setCurrentSession(id: string | null) {
    currentSessionId.value = id
    // 更新 isActive 状态
    sessionList.value = sessionList.value.map(s => ({
      ...s,
      isActive: s.id === id
    }))
  }

  function setSessions(sessions: SessionItem[]) {
    sessionList.value = sessions.map(s => ({
      ...s,
      isActive: s.id === currentSessionId.value
    }))
  }

  function addSession(session: SessionItem) {
    sessionList.value.unshift({
      ...session,
      isActive: false
    })
  }

  function updateSession(id: string, updates: Partial<SessionItem>) {
    const index = sessionList.value.findIndex(s => s.id === id)
    if (index !== -1) {
      sessionList.value[index] = { ...sessionList.value[index], ...updates }
    }
  }

  function removeSession(id: string) {
    const index = sessionList.value.findIndex(s => s.id === id)
    if (index !== -1) {
      sessionList.value.splice(index, 1)
      // 如果删除的是当前会话，切换到第一个
      if (currentSessionId.value === id) {
        currentSessionId.value = sessionList.value[0]?.id || null
      }
    }
  }

  function setLoading(isLoading: boolean) {
    loading.value = isLoading
  }

  function clear() {
    currentSessionId.value = null
    sessionList.value = []
  }

  return {
    // State
    currentSessionId,
    sessionList,
    loading,
    // Getters
    currentSession,
    sortedSessions,
    // Actions
    setCurrentSession,
    setSessions,
    addSession,
    updateSession,
    removeSession,
    setLoading,
    clear
  }
})
