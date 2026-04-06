<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ChatMessage } from '@/types'
import Avatar from '@/components/ui/Avatar.vue'
import { useUserStore } from '@/stores'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

interface Props {
  message: ChatMessage
}

const props = defineProps<Props>()
const userStore = useUserStore()

const isUser = computed(() => props.message.role === 'user')
const isStreaming = computed(() => props.message.status === 'streaming')

// 初始化 MarkdownIt，支持流式输出中的中间态
const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>';
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
  }
})

// 使用 computed 替代 ref + watch，确保内容更新时同步触发重新渲染
const renderedContent = computed(() => {
  let content = props.message.content || ''
  if (isUser.value) {
    return content
  }
  
  // 预处理 1：修复部分 LLM 返回的结构化标签前缺失换行导致的渲染失效
  // 特别针对 ### 标题和 * 列表等
  content = content
    .replace(/^#+(?=[^\s#])/gm, '$& ') // 修复 #Title -> # Title
    .replace(/^( {0,3})([*+-]|\d+\.)(?=[^\s])/gm, '$1$2 ') // 修复 -Item -> - Item
  
  // 预处理 2：流式补全逻辑
  let markdownToRender = content
  if (isStreaming.value) {
    // 自动闭合代码块
    const codeBlockCount = (content.match(/```/g) || []).length
    if (codeBlockCount % 2 !== 0) {
      markdownToRender += '\n```'
    }
  }

  // 渲染 Markdown
  let html = md.render(markdownToRender)
  
  // 预处理 3：修正流式末尾空白
  if (isStreaming.value && content.endsWith('\n') && !html.endsWith('<br>') && !html.endsWith('</p>')) {
    html += '<br>'
  }
  
  return html
})

// 展示引用
const showSources = ref(false)
</script>

<template>
  <div class="message-bubble" :class="{ user: isUser, assistant: !isUser, streaming: isStreaming }">
    <div class="message-avatar">
      <Avatar
        v-if="isUser"
        :name="userStore.userName"
        size="md"
      />
      <div v-else class="ai-avatar">
        <svg viewBox="0 0 32 32" fill="none">
          <path d="M16 2L28 9V23L16 30L4 23V9L16 2Z" fill="url(#ai-avatar-grad)" />
          <path d="M16 8L22 11.5V18.5L16 22L10 18.5V11.5L16 8Z" fill="var(--bg-secondary)" />
          <defs>
            <linearGradient id="ai-avatar-grad" x1="4" y1="2" x2="28" y2="30">
              <stop offset="0%" stop-color="#7c3aed" />
              <stop offset="100%" stop-color="#3b82f6" />
            </linearGradient>
          </defs>
        </svg>
      </div>
    </div>

    <div class="message-content-wrapper">
      <div class="message-header">
        <span class="message-role">{{ isUser ? userStore.userName : 'Guide RAG' }}</span>
      </div>

      <div class="message-content markdown-body" :class="{ 'assistant-md': !isUser }">
        <div class="markdown-content">
          <div v-html="renderedContent" class="rendered-html"></div>
          <span v-if="isStreaming" class="streaming-cursor">▊</span>
        </div>
      </div>

      <!-- 引用来源 -->
      <div v-if="message.sources && message.sources.length > 0" class="message-sources">
        <button class="sources-toggle" @click="showSources = !showSources">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
          {{ message.sources.length }} 个引用来源
          <svg 
            class="chevron"
            :class="{ expanded: showSources }"
            viewBox="0 0 24 24" 
            fill="none" 
            stroke="currentColor" 
            stroke-width="2"
          >
            <path d="M6 9l6 6 6-6"/>
          </svg>
        </button>
        
        <Transition name="sources">
          <div v-if="showSources" class="sources-list">
            <div
              v-for="(source, index) in message.sources"
              :key="index"
              class="source-item"
            >
              <span class="source-title">{{ source.title }}</span>
              <span v-if="source.content" class="source-content">
                {{ source.content }}
              </span>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-bubble {
  display: flex;
  gap: 1.25rem; /* 稍微增加间距 */
  max-width: 100%;
  margin-bottom: 1.5rem; /* 消息之间增加间距避免拥挤 */
}

.message-bubble.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: 10px;
}

.ai-avatar svg {
  width: 24px;
  height: 24px;
}

.message-content-wrapper {
  flex: 1;
  min-width: 0;
}

.message-header {
  margin-bottom: 0.5rem;
}

.message-role {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-tertiary);
}

/* Markdown 容器 */
.message-content {
  color: var(--text-primary);
  line-height: 1.7; /* 提高行高，改善大段文字拥挤 */
  font-size: 0.95rem;
  word-break: break-word;
}

.assistant-md {
  width: 100%;
}

.markdown-content {
  display: block;
  position: relative;
}

.rendered-html {
  display: block;
}

/* 强制覆盖最后一行内联逻辑，仅在非流式期间或非复合块时使用 */
.rendered-html :deep(p:last-child) {
  display: inline;
  margin-bottom: 0;
}

/* 用户消息样式 */
.user .message-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user .message-content {
  padding: 0.875rem 1.125rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 14px;
  white-space: pre-wrap; 
  display: inline-block; /* 用户消息包裹内容 */
  max-width: 85%;
}

/* Markdown 深度样式定义 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 1.5rem 0 1rem; /* 增加上下外边距防止挤在一起 */
  color: var(--text-primary);
  font-weight: 700;
  line-height: 1.4;
  display: block;
}

.markdown-body :deep(h1) { font-size: 1.75rem; border-bottom: 2px solid var(--border-primary); padding-bottom: 8px; }
.markdown-body :deep(h2) { font-size: 1.5rem; border-bottom: 1px solid var(--border-primary); padding-bottom: 6px; }
.markdown-body :deep(h3) { 
  font-size: 1.25rem; 
  padding-left: 10px;
  border-left: 4px solid var(--brand-primary); /* 为三级标题增加侧边条点缀，截图中最缺这个视觉差异 */
}
.markdown-body :deep(h4) { font-size: 1.1rem; }

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 1rem;
}

.markdown-body :deep(strong) {
  color: var(--text-primary);
  font-weight: 700;
  background: rgba(124, 58, 237, 0.05); /* 微弱底色强化重点 */
  padding: 0 2px;
}

/* 列表样式优化 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.75rem 0 1rem;
  padding-left: 1.5rem;
}

.markdown-body :deep(li) {
  margin-bottom: 0.5rem;
  display: list-item;
}

/* 表格深度优化 - 解决截图中的表格失效问题 */
.markdown-body :deep(table) {
  width: 100%;
  display: block; /* 在移动端或窄容器内允许横向滚动 */
  overflow-x: auto;
  border-collapse: collapse;
  margin: 1.25rem 0;
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 10px 14px;
  border: 1px solid var(--border-primary);
  text-align: left;
  font-size: 0.9rem;
}

.markdown-body :deep(th) {
  background: var(--bg-tertiary);
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-body :deep(tr:nth-child(even)) {
  background: rgba(0, 0, 0, 0.02);
}

/* 代码块样式 */
.markdown-body :deep(code) {
  padding: 3px 6px;
  background: var(--bg-tertiary);
  border-radius: 6px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.85em;
  color: var(--brand-primary);
}

.markdown-body :deep(pre) {
  margin: 1.25rem 0;
  padding: 1.25rem;
  background: #1e1e1e; /* 强制暗色背景呈现代码块，更专业 */
  border-radius: 12px;
  overflow-x: auto;
  box-shadow: var(--shadow-md);
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #d1d5db; /* 浅色文字 */
  font-size: 0.9rem;
}

/* 引用块 */
.markdown-body :deep(blockquote) {
  margin: 1.25rem 0;
  padding: 0.75rem 1.25rem;
  border-left: 4px solid var(--border-secondary);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border-radius: 0 8px 8px 0;
  font-style: italic;
}

.markdown-body :deep(hr) {
  margin: 2rem 0;
  border: 0;
  border-top: 1px solid var(--border-primary);
}

/* 流式光标样式 */
.streaming-cursor {
  display: inline-block;
  vertical-align: middle;
  margin-left: 2px;
  color: var(--brand-primary);
  animation: blink 1s step-end infinite;
  font-size: 1.2em;
  line-height: 1;
}

@keyframes blink {
  50% { opacity: 0; }
}

/* 引用来源卡片 */
.message-sources {
  margin-top: 1rem;
}

.sources-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: rgba(124, 58, 237, 0.08);
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 10px;
  color: var(--brand-primary);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.sources-toggle:hover {
  background: rgba(124, 58, 237, 0.12);
  transform: translateY(-1px);
}

.sources-toggle svg {
  width: 16px;
  height: 16px;
}

.sources-toggle .chevron {
  transition: transform 0.2s;
}

.sources-toggle .chevron.expanded {
  transform: rotate(180deg);
}

.sources-list {
  margin-top: 0.75rem;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 0.75rem;
}

.source-item {
  padding: 0.875rem 1.125rem;
  background: var(--bg-elevated);
  border: 1px solid var(--border-primary);
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
}

.source-title {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.source-content {
  display: block;
  font-size: 0.8rem;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 动画 */
.sources-enter-active,
.sources-leave-active {
  transition: all 0.2s ease;
}

.sources-enter-from,
.sources-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
