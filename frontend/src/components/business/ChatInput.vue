<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  loading?: boolean
  disabled?: boolean
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  disabled: false,
  placeholder: '输入消息，按 Enter 发送...'
})

const emit = defineEmits<{
  send: [message: string]
  abort: []
}>()

const inputText = ref('')
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const canSend = computed(() => inputText.value.trim().length > 0 && !props.loading && !props.disabled)

// 自动调整高度
function adjustHeight() {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
  }
}

// 处理输入
function handleInput() {
  adjustHeight()
}

// 处理按键
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

// 发送消息
function handleSend() {
  if (!canSend.value) return
  
  const message = inputText.value.trim()
  emit('send', message)
  inputText.value = ''
  
  // 重置高度
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

// 取消响应
function handleAbort() {
  emit('abort')
}

// 文件上传（预留）
const fileInputRef = ref<HTMLInputElement | null>(null)

function handleFileClick() {
  fileInputRef.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    // TODO: 处理文件上传
    console.log('Files:', target.files)
  }
}
</script>

<template>
  <div class="chat-input">
    <div class="input-container">
      <!-- 附件按钮 -->
      <button class="attach-btn" @click="handleFileClick" title="上传文件">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
        </svg>
      </button>
      <input
        ref="fileInputRef"
        type="file"
        multiple
        accept=".pdf,.doc,.docx,.md,.txt,image/*"
        style="display: none;"
        @change="handleFileChange"
      />

      <!-- 输入框 -->
      <textarea
        ref="textareaRef"
        v-model="inputText"
        class="input-textarea"
        :placeholder="placeholder"
        :disabled="disabled"
        rows="1"
        @input="handleInput"
        @keydown="handleKeydown"
      />

      <!-- 发送/停止按钮 -->
      <button
        v-if="loading"
        class="stop-btn"
        @click="handleAbort"
        title="停止生成"
      >
        <svg viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" rx="2"/>
        </svg>
      </button>
      <button
        v-else
        class="send-btn"
        :class="{ active: canSend }"
        :disabled="!canSend"
        @click="handleSend"
        title="发送消息"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
      </button>
    </div>

    <p class="input-hint">
      <kbd>Enter</kbd> 发送，<kbd>Shift + Enter</kbd> 换行
    </p>
  </div>
</template>

<style scoped>
.chat-input {
  max-width: 900px;
  margin: 0 auto;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  transition: border-color 0.2s;
}

.input-container:focus-within {
  border-color: var(--brand-primary);
}

.attach-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.attach-btn:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.attach-btn svg {
  width: 20px;
  height: 20px;
}

.input-textarea {
  flex: 1;
  min-height: 36px;
  max-height: 200px;
  padding: 0.5rem 0;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 0.95rem;
  line-height: 1.5;
  resize: none;
  outline: none;
}

.input-textarea::placeholder {
  color: var(--text-tertiary);
}

.input-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn,
.stop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  background: var(--bg-tertiary);
  border: none;
  border-radius: 8px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:disabled {
  cursor: not-allowed;
}

.send-btn.active {
  background: var(--brand-primary);
  color: white;
}

.send-btn.active:hover {
  transform: scale(1.05);
}

.stop-btn {
  background: var(--error);
  color: white;
}

.stop-btn:hover {
  background: #dc2626;
}

.send-btn svg,
.stop-btn svg {
  width: 18px;
  height: 18px;
}

.input-hint {
  margin: 0.5rem 0 0;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  text-align: center;
}

.input-hint kbd {
  padding: 0.125rem 0.375rem;
  background: var(--bg-tertiary);
  border-radius: 4px;
  font-family: inherit;
  font-size: inherit;
}
</style>
