import { ref, onUnmounted } from 'vue'
import { knowledgeBaseApi } from '@/api/modules'
import { useKnowledgeBaseStore } from '@/stores'

interface UseDocumentPollingOptions {
  interval?: number // 轮询间隔，默认 2000ms
  maxDuration?: number // 最大轮询时长，默认 5 分钟
}

export function useDocumentPolling(
  getKnowledgeBaseId: () => string | null,
  options: UseDocumentPollingOptions = {}
) {
  const { interval = 2000, maxDuration = 5 * 60 * 1000 } = options
  
  const kbStore = useKnowledgeBaseStore()
  const isPolling = ref(false)
  
  let timerId: number | null = null
  let startTime: number = 0

  /**
   * 检查是否有需要轮询的文档
   */
  function hasProcessingDocuments(): boolean {
    return kbStore.processingDocuments.length > 0
  }

  /**
   * 轮询单个文档状态
   */
  async function pollDocument(documentId: string): Promise<boolean> {
    if (!documentId || documentId === 'undefined') {
      console.warn('[Polling] 无效的文档 ID，跳过轮询:', documentId)
      return false
    }
    
    try {
      const knowledgeBaseId = getKnowledgeBaseId()
      if (!knowledgeBaseId) {
        return false
      }
      const response = await knowledgeBaseApi.getDocumentStatus(knowledgeBaseId, documentId)
      const data = response.data
      kbStore.updateDocument(documentId, {
        status: data.status,
        progress: data.progress,
        errorMessage: data.errorMessage || undefined
      })
      
      // 返回是否还需要继续轮询
      return data.status === 'PENDING' || data.status === 'PARSING'
    } catch (error) {
      console.error('Failed to poll document status:', error)
    }
    return false
  }

  /**
   * 执行一次轮询
   */
  async function poll() {
    const processingDocs = kbStore.processingDocuments
    
    if (processingDocs.length === 0) {
      stopPolling()
      return
    }

    // 检查是否超时
    if (Date.now() - startTime > maxDuration) {
      console.warn('Document polling timeout')
      stopPolling()
      return
    }

    // 轮询所有处理中的文档
    const results = await Promise.all(
      processingDocs.map(doc => pollDocument(doc.id))
    )

    // 如果还有需要继续轮询的文档，继续轮询
    if (results.some(needsPolling => needsPolling)) {
      scheduleNextPoll()
    } else {
      stopPolling()
    }
  }

  /**
   * 调度下一次轮询
   */
  function scheduleNextPoll() {
    if (timerId) return
    timerId = window.setTimeout(() => {
      timerId = null
      poll()
    }, interval)
  }

  /**
   * 开始轮询
   */
  function startPolling() {
    if (isPolling.value) return
    
    const processingCount = kbStore.processingDocuments.length
    if (processingCount === 0) return

    console.log(`Starting document polling for ${processingCount} documents`)
    isPolling.value = true
    startTime = Date.now()
    poll()
  }

  /**
   * 停止轮询
   */
  function stopPolling() {
    isPolling.value = false
    if (timerId) {
      clearTimeout(timerId)
      timerId = null
    }
  }

  // 组件卸载时停止轮询
  onUnmounted(() => {
    stopPolling()
  })

  return {
    isPolling,
    startPolling,
    stopPolling,
    hasProcessingDocuments
  }
}
