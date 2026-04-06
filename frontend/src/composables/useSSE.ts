import { ref, onUnmounted } from 'vue'
import { useChatStore, useModelConfigStore } from '@/stores'
import { createChatStream, type SSEStartData, type SSECitationData, type SSEDoneData, type SSEErrorData } from '@/api'
import type { ChatMessage } from '@/types'

interface UseSSEOptions {
  onStart?: (messageId: string) => void
  onComplete?: () => void
  onError?: (error: string) => void
}

export function useSSE(options: UseSSEOptions = {}) {
  const chatStore = useChatStore()
  const modelConfigStore = useModelConfigStore()
  const abortController = ref<{ abort: () => void } | null>(null)
  const isStreaming = ref(false)

  /**
   * 发送消息并处理流式响应
   * 使用真实的 SSE API: POST /rag/v1/chat/stream
   */
  async function sendMessage(sessionId: string, message: string, kbId?: string) {
    if (isStreaming.value) {
      console.warn('Already streaming, please wait...')
      return
    }

    isStreaming.value = true

    // 添加用户消息
    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: message,
      status: 'completed',
      createdAt: new Date().toISOString()
    }
    chatStore.appendMessage(userMessage)

    // 创建助手消息占位（ID 会在收到 start 事件后更新）
    let assistantMessageId = `assistant-${Date.now()}`
    const assistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      status: 'streaming',
      createdAt: new Date().toISOString()
    }
    chatStore.appendMessage(assistantMessage)
    chatStore.startStreaming(assistantMessageId)
    options.onStart?.(assistantMessageId)

    try {
      // 使用真实的 SSE 流式 API
      const { promise, abort } = createChatStream({
        payload: {
          sessionId,
          query: message,
          kbId,
          modelOverride: modelConfigStore.modelOverridePayload
        },
        onStart: (data: SSEStartData) => {
          // 更新消息 ID 为后端返回的 ID
          if (data.messageId) {
            chatStore.updateMessage(assistantMessageId, { id: data.messageId })
            assistantMessageId = data.messageId
          }
        },
        onDelta: (text: string) => {
          if (text) {
            chatStore.patchAssistantContent(assistantMessageId, text)
          }
        },
        onCitation: (data: SSECitationData) => {
          // 处理引用数据
          if (data.citations) {
            for (const citation of data.citations) {
              chatStore.addCitation(assistantMessageId, {
                title: citation.sourceText?.slice(0, 50) || citation.docId,
                chunkId: citation.chunkId,
                content: citation.sourceText,
                score: citation.score
              })
            }
          }
        },
        onDone: (_data: SSEDoneData) => {
          chatStore.setMessageStatus(assistantMessageId, 'completed')
          options.onComplete?.()
        },
        onError: (error: SSEErrorData) => {
          console.error('SSE Error:', error)
          chatStore.setMessageStatus(assistantMessageId, 'error')
          chatStore.patchAssistantContent(assistantMessageId, `\n\n⚠️ 错误: ${error.message}`)
          options.onError?.(error.message)
        }
      })

      abortController.value = { abort }
      await promise
    } catch (error) {
      // 如果是手动中止，不显示错误
      if (error instanceof Error && error.name === 'SSEAbortError') {
        chatStore.setMessageStatus(assistantMessageId, 'completed')
        chatStore.patchAssistantContent(assistantMessageId, '\n\n[已停止生成]')
      } else {
        console.error('SSE Error:', error)
        chatStore.setMessageStatus(assistantMessageId, 'error')
        options.onError?.(error instanceof Error ? error.message : '发送失败')
      }
    } finally {
      isStreaming.value = false
      chatStore.stopStreaming()
      abortController.value = null
    }
  }

  /**
   * 取消当前流式请求
   */
  function abort() {
    if (abortController.value) {
      abortController.value.abort()
      isStreaming.value = false
      chatStore.stopStreaming()
    }
  }

  // 组件卸载时自动取消
  onUnmounted(() => {
    abort()
  })

  return {
    isStreaming,
    sendMessage,
    abort
  }
}
