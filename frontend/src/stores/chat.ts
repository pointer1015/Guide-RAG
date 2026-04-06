import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { ChatMessage, ChatSource } from '@/types'

export const useChatStore = defineStore('chat', () => {
  // State
  const messages = ref<ChatMessage[]>([])
  const streaming = ref(false)
  const loading = ref(false)
  const currentStreamingMessageId = ref<string | null>(null)

  // Getters
  const messageCount = computed(() => messages.value.length)
  
  const lastMessage = computed(() => 
    messages.value.length > 0 ? messages.value[messages.value.length - 1] : null
  )

  const isStreaming = computed(() => streaming.value)

  // Actions
  function setMessages(newMessages: ChatMessage[]) {
    messages.value = newMessages
  }

  function appendMessage(message: ChatMessage) {
    messages.value.push(message)
  }

  function updateMessage(id: string, updates: Partial<ChatMessage>) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      messages.value[index] = { ...messages.value[index], ...updates }
    }
  }

  function patchAssistantContent(id: string, chunk: string) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      // 这里的逻辑：在 Vue 3 中，直接修改响应式数组中对象的属性是可追踪的
      // 但为了确保万无一失（某些版本的 Vite/Vue 反射机制问题），我们显式触发一次引用更新
      const msg = messages.value[index]
      msg.content += chunk
      // 强制触发数组层级的响应式通知
      messages.value[index] = { ...msg }
    }
  }

  function addCitation(id: string, citation: ChatSource) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      const msg = messages.value[index]
      if (!msg.sources) {
        msg.sources = []
      }
      msg.sources.push(citation)
      messages.value[index] = { ...msg }
    }
  }

  function setMessageStatus(id: string, status: ChatMessage['status']) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      messages.value[index] = { ...messages.value[index], status }
    }
  }

  function removeMessage(id: string) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
  }

  function startStreaming(messageId: string) {
    streaming.value = true
    currentStreamingMessageId.value = messageId
  }

  function stopStreaming() {
    streaming.value = false
    if (currentStreamingMessageId.value) {
      setMessageStatus(currentStreamingMessageId.value, 'completed')
      currentStreamingMessageId.value = null
    }
  }

  function setLoading(isLoading: boolean) {
    loading.value = isLoading
  }

  function clear() {
    messages.value = []
    streaming.value = false
    loading.value = false
    currentStreamingMessageId.value = null
  }

  return {
    // State
    messages,
    streaming,
    loading,
    currentStreamingMessageId,
    // Getters
    messageCount,
    lastMessage,
    isStreaming,
    // Actions
    setMessages,
    appendMessage,
    updateMessage,
    patchAssistantContent,
    addCitation,
    setMessageStatus,
    removeMessage,
    startStreaming,
    stopStreaming,
    setLoading,
    clear
  }
})
