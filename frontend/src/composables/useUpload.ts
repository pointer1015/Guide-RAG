import { ref, computed } from 'vue'
import { knowledgeBaseApi } from '@/api/modules'
import { useKnowledgeBaseStore } from '@/stores'
import type { UploadProgress } from '@/types'

const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
const ACCEPTED_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/markdown',
  'text/plain',
  'image/png',
  'image/jpeg',
  'image/webp',
  'audio/mpeg',
  'audio/wav',
  'audio/x-m4a'
]

export function useUpload(getKnowledgeBaseId: () => string | null) {
  const kbStore = useKnowledgeBaseStore()
  
  const uploadQueue = ref<UploadProgress[]>([])
  const isUploading = ref(false)

  const pendingCount = computed(() => 
    uploadQueue.value.filter(u => u.status === 'uploading').length
  )

  /**
   * 验证文件
   */
  function validateFile(file: File): string | null {
    if (file.size > MAX_FILE_SIZE) {
      return `文件 "${file.name}" 超过 50MB 限制`
    }
    if (!ACCEPTED_TYPES.includes(file.type)) {
      return `文件 "${file.name}" 格式不支持`
    }
    return null
  }

  /**
   * 上传单个文件
   */
  async function uploadFile(file: File): Promise<boolean> {
    const knowledgeBaseId = getKnowledgeBaseId()
    if (!knowledgeBaseId) {
      console.error('未选择知识库，无法上传文档')
      return false
    }

    const error = validateFile(file)
    if (error) {
      console.error(error)
      return false
    }

    const fileId = `upload-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    
    // 添加到上传队列
    uploadQueue.value.push({
      fileId,
      fileName: file.name,
      progress: 0,
      status: 'uploading'
    })

    // 添加到 store（前端临时状态）
    kbStore.addDocument({
      id: fileId,
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'uploading',
      progress: 0,
      createdAt: new Date().toISOString()
    })

    try {
      const response = await knowledgeBaseApi.uploadDocument(
        knowledgeBaseId,
        file,
        {
          onProgress: (progress) => {
            // 更新上传进度
            const item = uploadQueue.value.find(u => u.fileId === fileId)
            if (item) {
              item.progress = progress
            }
            kbStore.updateDocument(fileId, { progress })
          }
        }
      )

      // 更新状态
      const item = uploadQueue.value.find(u => u.fileId === fileId)
      if (item) {
        item.status = 'success'
        item.progress = 100
      }
      
      // 更新 store 中的文档 ID 和状态
      // 后端 Result<Long> 直接返回 ID
      const newDocId = String(response.data)
      kbStore.updateDocument(fileId, {
        id: newDocId,
        status: 'PENDING',
        progress: 0
      })
      
      return true
    } catch (error) {
      const item = uploadQueue.value.find(u => u.fileId === fileId)
      if (item) {
        item.status = 'error'
        item.errorMessage = error instanceof Error ? error.message : '上传失败'
      }
      kbStore.updateDocument(fileId, { 
        status: 'FAILED',
        errorMessage: error instanceof Error ? error.message : '上传失败'
      })
      return false
    }
  }

  /**
   * 批量上传文件
   */
  async function uploadFiles(files: FileList | File[]): Promise<void> {
    isUploading.value = true
    
    const fileArray = Array.from(files)
    
    // 串行上传，避免并发过多
    for (const file of fileArray) {
      await uploadFile(file)
    }
    
    isUploading.value = false
  }

  /**
   * 清除已完成的上传记录
   */
  function clearCompleted() {
    uploadQueue.value = uploadQueue.value.filter(
      u => u.status === 'uploading'
    )
  }

  /**
   * 重试失败的上传
   */
  async function retryFailed(fileId: string, file: File) {
    const item = uploadQueue.value.find(u => u.fileId === fileId)
    if (item) {
      item.status = 'uploading'
      item.progress = 0
      item.errorMessage = undefined
      await uploadFile(file)
    }
  }

  return {
    uploadQueue,
    isUploading,
    pendingCount,
    validateFile,
    uploadFile,
    uploadFiles,
    clearCompleted,
    retryFailed
  }
}
