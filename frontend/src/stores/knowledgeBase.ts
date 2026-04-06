import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { KnowledgeBase, KbDocument } from '@/types'

export const useKnowledgeBaseStore = defineStore('knowledgeBase', () => {
  // State
  const currentKbId = ref<string | null>(null)
  const knowledgeBases = ref<KnowledgeBase[]>([])
  const documents = ref<KbDocument[]>([])
  const loading = ref(false)
  const documentsLoading = ref(false)

  // Getters
  const currentKb = computed(() => 
    knowledgeBases.value.find(kb => kb.id === currentKbId.value) || null
  )

  const sortedDocuments = computed(() => 
    [...documents.value].sort((a, b) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  )

  const parsedDocuments = computed(() => 
    documents.value.filter(d => d.status === 'PARSED')
  )

  const processingDocuments = computed(() => 
    documents.value.filter(d => d.status === 'PENDING' || d.status === 'PARSING' || d.status === 'uploading')
  )

  // Actions
  function setCurrentKb(id: string | null) {
    currentKbId.value = id
  }

  function setKnowledgeBases(kbs: KnowledgeBase[]) {
    knowledgeBases.value = kbs
  }

  function addKnowledgeBase(kb: KnowledgeBase) {
    knowledgeBases.value.unshift(kb)
  }

  function updateKnowledgeBase(id: string, updates: Partial<KnowledgeBase>) {
    const index = knowledgeBases.value.findIndex(kb => kb.id === id)
    if (index !== -1) {
      knowledgeBases.value[index] = { ...knowledgeBases.value[index], ...updates }
    }
  }

  function removeKnowledgeBase(id: string) {
    const index = knowledgeBases.value.findIndex(kb => kb.id === id)
    if (index !== -1) {
      knowledgeBases.value.splice(index, 1)
      if (currentKbId.value === id) {
        currentKbId.value = knowledgeBases.value[0]?.id || null
      }
    }
  }

  function setDocuments(docs: KbDocument[]) {
    documents.value = docs
  }

  function addDocument(doc: KbDocument) {
    documents.value.unshift(doc)
  }

  function updateDocument(id: string, updates: Partial<KbDocument>) {
    const index = documents.value.findIndex(d => d.id === id)
    if (index !== -1) {
      // 如果提供了新的 id，我们需要更新整个对象，包括它的 id 字段
      const updatedDoc = { ...documents.value[index], ...updates }
      documents.value[index] = updatedDoc
    }
  }

  function removeDocument(id: string) {
    const index = documents.value.findIndex(d => d.id === id)
    if (index !== -1) {
      documents.value.splice(index, 1)
    }
  }

  function setLoading(isLoading: boolean) {
    loading.value = isLoading
  }

  function setDocumentsLoading(isLoading: boolean) {
    documentsLoading.value = isLoading
  }

  function clear() {
    currentKbId.value = null
    knowledgeBases.value = []
    documents.value = []
    loading.value = false
    documentsLoading.value = false
  }

  return {
    // State
    currentKbId,
    knowledgeBases,
    documents,
    loading,
    documentsLoading,
    // Getters
    currentKb,
    sortedDocuments,
    parsedDocuments,
    processingDocuments,
    // Actions
    setCurrentKb,
    setKnowledgeBases,
    addKnowledgeBase,
    updateKnowledgeBase,
    removeKnowledgeBase,
    setDocuments,
    addDocument,
    updateDocument,
    removeDocument,
    setLoading,
    setDocumentsLoading,
    clear
  }
})
