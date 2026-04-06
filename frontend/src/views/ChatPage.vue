<script setup lang="ts">
import { ref, watch, computed, nextTick, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useChatStore, useSessionStore, useKnowledgeBaseStore } from "@/stores";
import { useSSE, useAutoScroll, useTheme } from "@/composables";
import { chatApi, knowledgeBaseApi } from "@/api/modules";
import MessageBubble from "@/components/business/MessageBubble.vue";
import ChatInput from "@/components/business/ChatInput.vue";

const route = useRoute();
const router = useRouter();
const chatStore = useChatStore();
const sessionStore = useSessionStore();
const kbStore = useKnowledgeBaseStore();
const { isDark } = useTheme();

const messageContainerRef = ref<HTMLElement | null>(null);
const { scrollToBottom, resetScroll } = useAutoScroll(messageContainerRef);

// SSE 处理
const { isStreaming, sendMessage, abort } = useSSE({
    onStart: () => {
        nextTick(() => scrollToBottom(true));
    },
    onComplete: () => {
        nextTick(() => {
            scrollToBottom(true);
            // 确保最后一次渲染已完成
            chatStore.setLoading(false);
        });
    },
    onError: (error) => {
        console.error("Chat error:", error);
    },
});

// 当前会话 ID
const currentSessionId = computed(() => {
    const id = route.params.id;
    if (!id || id === "undefined") return undefined;
    return String(id);
});

// 当前选择的知识库
const selectedKbId = ref<string | null>(null);
const isKbDropdownOpen = ref(false);

function toggleKbDropdown() {
    isKbDropdownOpen.value = !isKbDropdownOpen.value;
}

function selectKb(id: string) {
    selectedKbId.value = id;
    isKbDropdownOpen.value = false;
}

// 空状态
const isEmpty = computed(() => chatStore.messages.length === 0);

// 加载会话消息
async function loadMessages(sessionId: string) {
    chatStore.setLoading(true);
    chatStore.clear();
    resetScroll();

    try {
        const response = await chatApi.getMessages(sessionId);
        // 映射后端响应格式到前端格式
        chatStore.setMessages(
            (response.data.list || []).map((m) => ({
                id: m.messageId,
                role: m.role,
                content: m.content,
                sources: m.citations?.map((c) => ({
                    title: c.sourceText?.slice(0, 30) || c.docId,
                    chunkId: c.chunkId,
                    content: c.sourceText,
                    score: c.score,
                })),
                status: "completed" as const,
                createdAt: m.gmtCreate,
            })),
        );
        nextTick(() => scrollToBottom(true));
    } catch (error) {
        console.error("Failed to load messages:", error);
    } finally {
        chatStore.setLoading(false);
    }
}

// 防止新建对话路由切换时把刚发送的消息清空
let isNavigatingToNewSession = false;

// 监听路由变化
watch(
    () => route.params.id,
    async (newId) => {
        if (newId && typeof newId === "string" && newId !== "undefined") {
            sessionStore.setCurrentSession(newId);
            if (isNavigatingToNewSession) {
                isNavigatingToNewSession = false;
                // 此时不调用 loadMessages 避免打断前端已经放进本地 store 并正在流式输出的消息
            } else {
                await loadMessages(newId);
            }
        }
    },
    { immediate: true },
);

// 监听知识库加载，默认选中第一个
watch(
    () => kbStore.knowledgeBases,
    (kbs) => {
        if (!selectedKbId.value && kbs && kbs.length > 0) {
            selectedKbId.value = kbs[0].id;
        }
    },
    { immediate: true },
);

onMounted(async () => {
    if (kbStore.knowledgeBases.length === 0) {
        try {
            const resp = await knowledgeBaseApi.getKnowledgeBases();
            const kbList = (resp.data.list || []).map((kb: any) => ({
                id: String(kb.id),
                name: kb.name,
                description: kb.description,
                documentCount: kb.docCount || 0,
                createdAt: kb.gmtCreate,
                updatedAt: kb.gmtModified || kb.gmtCreate
            }));
            kbStore.setKnowledgeBases(kbList);
            if (kbList.length > 0 && !selectedKbId.value) {
                selectedKbId.value = kbList[0].id;
            }
        } catch (e) {
            console.error("Failed to load kb: ", e);
        }
    }
});

// 发送消息
async function handleSend(message: string) {
    if (!message.trim() || isStreaming.value) return;

    // 如果没有当前会话，先创建一个
    let sessionId = currentSessionId.value;

    // 冗余校验：如果是从侧边栏点击后立即发送，确保获取的是最新的路由参数
    const routeId = route.params.id;
    if (routeId && routeId !== "undefined" && !sessionId) {
        sessionId = String(routeId);
    }

    if (!sessionId) {
        try {
            const response = await chatApi.createSession({
                title: "新对话",
                knowledgeBaseId: selectedKbId.value || undefined,
            });
            const newSessionId = String(response.data.id);
            sessionStore.addSession({
                id: newSessionId,
                title: response.data.title,
                updatedAt:
                    response.data.gmtModified ||
                    response.data.gmtCreate ||
                    new Date().toISOString(),
            });
            sessionStore.setCurrentSession(newSessionId);
            sessionId = newSessionId;
            isNavigatingToNewSession = true;
            // 使用 replace 避免新对话产生多余的历史记录
            await router.replace(`/chat/${sessionId}`);
        } catch (error) {
            console.error("Failed to create session:", error);
            return;
        }
    }

    const isFirstMessage = isEmpty.value;
    await sendMessage(sessionId!, message, selectedKbId.value || undefined);

    // 发送第一个消息后，如果标题还是“新对话”，则自动更新标题
    if (isFirstMessage && sessionId) {
        const newTitle = message.slice(0, 30) + (message.length > 30 ? "..." : "");
        try {
            await chatApi.updateSession(sessionId, { title: newTitle });
            sessionStore.updateSession(sessionId, { title: newTitle });
        } catch (e) {
            console.error("Failed to auto-update title:", e);
        }
    }
}

// 取消响应
function handleAbort() {
    abort();
}

// 示例问题
const exampleQuestions = [
    {
        icon: "chart",
        text: "新员工入职流程有哪些步骤？",
    },
];

function askExample(question: string) {
    handleSend(question);
}
</script>

<template>
    <div class="chat-page" :class="{ 'is-dark': isDark }">
        <!-- 顶部工具栏（仅保留标题） -->
        <header class="chat-header">
            <div class="header-side left"></div>
            <h1 class="header-title">
                {{
                    currentSessionId
                        ? sessionStore.currentSession?.title || "新对话"
                        : "新对话"
                }}
            </h1>
            <div class="header-side right">
                <div class="kb-selector">
                    <button
                        class="kb-trigger"
                        :class="{ open: isKbDropdownOpen }"
                        @click="toggleKbDropdown"
                    >
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <path
                                d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"
                            />
                            <path
                                d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"
                            />
                        </svg>
                        <span class="kb-name">
                            {{
                                kbStore.knowledgeBases.find(
                                    (k) => k.id === selectedKbId,
                                )?.name || "选择知识库"
                            }}
                        </span>
                        <svg
                            class="chevron"
                            :class="{ open: isKbDropdownOpen }"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <path d="M6 9l6 6 6-6" />
                        </svg>
                    </button>

                    <Transition name="dropdown">
                        <div v-if="isKbDropdownOpen" class="kb-dropdown glass">
                            <div class="dropdown-header">选择知识库</div>
                            <div class="kb-options">
                                <button
                                    v-for="kb in kbStore.knowledgeBases"
                                    :key="kb.id"
                                    class="kb-option"
                                    :class="{
                                        active: selectedKbId === kb.id,
                                    }"
                                    @click="selectKb(kb.id)"
                                >
                                    <svg
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        stroke-width="2"
                                    >
                                        <path
                                            d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"
                                        />
                                        <path
                                            d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"
                                        />
                                    </svg>
                                    <span class="kb-option-name">{{
                                        kb.name
                                    }}</span>
                                    <span class="kb-doc-count"
                                        >{{ kb.documentCount }} 文档</span
                                    >
                                </button>
                            </div>
                        </div>
                    </Transition>
                </div>
            </div>
        </header>

        <!-- 消息列表 -->
        <div
            ref="messageContainerRef"
            class="message-container"
            :class="{ empty: isEmpty }"
        >
            <!-- 空状态 -->
            <div v-if="isEmpty && !chatStore.loading" class="empty-state">
                <div class="empty-visual">
                    <div class="visual-circle"></div>
                    <div class="visual-icon">
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="1.5"
                        >
                            <path
                                d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
                            />
                        </svg>
                    </div>
                </div>

                <h2 class="empty-title">开始智能对话</h2>
                <p class="empty-desc">
                    基于 RAG 技术，AI
                    将从知识库中检索相关内容，为您提供精准、有据可依的回答
                </p>

                <div class="example-grid">
                    <button
                        v-for="(q, i) in exampleQuestions"
                        :key="i"
                        class="example-card"
                        @click="askExample(q.text)"
                    >
                        <div class="example-icon">
                            <svg
                                v-if="q.icon === 'chart'"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="2"
                            >
                                <line x1="18" y1="20" x2="18" y2="10" />
                                <line x1="12" y1="20" x2="12" y2="4" />
                                <line x1="6" y1="20" x2="6" y2="14" />
                            </svg>
                            <svg
                                v-else-if="q.icon === 'settings'"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="2"
                            >
                                <circle cx="12" cy="12" r="3" />
                                <path
                                    d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"
                                />
                            </svg>
                            <svg
                                v-else-if="q.icon === 'code'"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="2"
                            >
                                <polyline points="16 18 22 12 16 6" />
                                <polyline points="8 6 2 12 8 18" />
                            </svg>
                            <svg
                                v-else
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="2"
                            >
                                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                                <path
                                    d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"
                                />
                            </svg>
                        </div>
                        <span class="example-text">{{ q.text }}</span>
                        <svg
                            class="example-arrow"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <line x1="5" y1="12" x2="19" y2="12" />
                            <polyline points="12 5 19 12 12 19" />
                        </svg>
                    </button>
                </div>

                <div class="empty-tips">
                    <div class="tip">
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <path
                                d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"
                            />
                        </svg>
                        <span>回答基于知识库文档，安全可控</span>
                    </div>
                    <div class="tip">
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <polygon
                                points="13 2 3 14 12 14 11 22 21 10 12 10"
                            />
                        </svg>
                        <span>流式输出，实时显示回答过程</span>
                    </div>
                </div>
            </div>

            <!-- 加载中 -->
            <div v-else-if="chatStore.loading" class="loading-state">
                <div class="loading-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
                <span>加载消息中...</span>
            </div>

            <!-- 消息列表 -->
            <div v-else class="messages">
                <TransitionGroup name="message">
                    <MessageBubble
                        v-for="message in chatStore.messages"
                        :key="message.id"
                        :message="message"
                    />
                </TransitionGroup>

                <!-- 打字指示器 -->
                <div v-if="isStreaming" class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
            <div class="input-container">
                <ChatInput
                    :loading="isStreaming"
                    :disabled="chatStore.loading"
                    @send="handleSend"
                    @abort="handleAbort"
                />
                <p class="input-hint">RAG 智库可能会出错，请核实重要信息</p>
            </div>
        </div>
    </div>
</template>

<style scoped>
.chat-page {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: var(--bg-primary);
}

/* Header */
.chat-header {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    padding: 12px 24px;
    border-bottom: 1px solid var(--border-primary);
    background: var(--bg-primary);
}

.header-side {
    display: flex;
    align-items: center;
}

.header-side.right {
    justify-content: flex-end;
}

.header-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    max-width: 400px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: center;
}

/* 知识库选择器 */
.kb-selector {
    position: relative;
}

.kb-trigger {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: var(--bg-tertiary);
    border: 1px solid var(--border-primary);
    border-radius: 10px;
    color: var(--text-secondary);
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
}

.kb-trigger:hover {
    background: var(--bg-hover);
    border-color: var(--border-secondary);
    color: var(--text-primary);
}

.kb-trigger svg {
    width: 16px;
    height: 16px;
}

.kb-trigger .chevron {
    width: 14px;
    height: 14px;
    transition: transform 0.2s;
}

.kb-trigger .chevron.open {
    transform: rotate(180deg);
}

.kb-dropdown {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    min-width: 220px;
    padding: 8px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-primary);
    border-radius: 12px;
    box-shadow: var(--shadow-lg);
    z-index: 100;
}

.kb-option {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    padding: 10px 12px;
    background: transparent;
    border: none;
    border-radius: 8px;
    color: var(--text-secondary);
    font-size: 13px;
    text-align: left;
    cursor: pointer;
    transition: all 0.2s;
}

.kb-option:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.kb-option.active {
    background: var(--brand-primary);
    background: rgba(59, 130, 246, 0.1);
    color: var(--brand-primary);
}

.kb-option svg {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
}

.kb-options {
    max-height: 240px;
    overflow-y: auto;
}

.dropdown-header {
    padding: 8px 12px;
    font-size: 11px;
    font-weight: 600;
    color: var(--text-tertiary);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    border-bottom: 1px solid var(--border-primary);
    margin-bottom: 4px;
}

.kb-trigger.open {
    border-color: var(--brand-primary);
    background: var(--bg-hover);
    color: var(--text-primary);
}

.kb-name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.kb-option-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 动画补丁 */
.dropdown-enter-active,
.dropdown-leave-active {
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from,
.dropdown-leave-to {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
}

/* 消息容器 */
.message-container {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    background: var(--bg-primary);
}

.message-container.empty {
    display: flex;
    align-items: center;
    justify-content: center;
}

/* 空状态 */
.empty-state {
    text-align: center;
    max-width: 600px;
    padding: 40px 20px;
}

.empty-visual {
    position: relative;
    width: 120px;
    height: 120px;
    margin: 0 auto 32px;
}

.visual-circle {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: var(--brand-primary);
    opacity: 0.1;
    animation: pulse 3s ease-in-out infinite;
}

.visual-icon {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.visual-icon svg {
    width: 48px;
    height: 48px;
    color: var(--brand-primary);
}

.empty-title {
    margin: 0 0 12px;
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
}

.empty-desc {
    margin: 0 0 40px;
    font-size: 15px;
    line-height: 1.7;
    color: var(--text-secondary);
}

.example-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 40px;
}

.example-card {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
    background: var(--bg-secondary);
    border: 1px solid var(--border-primary);
    border-radius: 14px;
    text-align: left;
    cursor: pointer;
    transition: all 0.2s;
}

.example-card:hover {
    background: var(--bg-tertiary);
    border-color: var(--brand-primary);
    transform: translateY(-2px);
}

.example-card:hover .example-arrow {
    opacity: 1;
    transform: translateX(0);
}

.example-icon {
    flex-shrink: 0;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-secondary);
    border: 1px solid var(--border-primary);
    border-radius: 10px;
}

.example-icon svg {
    width: 18px;
    height: 18px;
    color: var(--brand-primary);
}

.example-text {
    flex: 1;
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-secondary);
}

.example-arrow {
    flex-shrink: 0;
    width: 16px;
    height: 16px;
    color: var(--brand-primary);
    opacity: 0;
    transform: translateX(-4px);
    transition: all 0.2s;
}

.empty-tips {
    display: flex;
    justify-content: center;
    gap: 32px;
}

.tip {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--text-tertiary);
}

.tip svg {
    width: 16px;
    height: 16px;
    color: var(--text-tertiary);
}

/* 加载状态 */
.loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 60px;
    color: var(--text-tertiary);
}

.loading-dots {
    display: flex;
    gap: 6px;
}

.loading-dots span {
    width: 8px;
    height: 8px;
    background: var(--brand-primary);
    border-radius: 50%;
    animation: loadingDot 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(2) {
    animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
    animation-delay: 0.4s;
}

@keyframes loadingDot {
    0%,
    80%,
    100% {
        transform: scale(0.6);
        opacity: 0.5;
    }
    40% {
        transform: scale(1);
        opacity: 1;
    }
}

/* 消息列表 */
.messages {
    display: flex;
    flex-direction: column;
    gap: 24px;
    max-width: 800px;
    margin: 0 auto;
}

/* 打字指示器 */
.typing-indicator {
    display: flex;
    gap: 4px;
    padding: 16px 20px;
    background: var(--bg-secondary);
    border-radius: 16px;
    width: fit-content;
}

.typing-indicator span {
    width: 8px;
    height: 8px;
    background: var(--text-tertiary);
    border-radius: 50%;
    animation: typing 1.4s ease-in-out infinite;
}

.typing-indicator span:nth-child(2) {
    animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
    animation-delay: 0.4s;
}

@keyframes typing {
    0%,
    60%,
    100% {
        transform: translateY(0);
    }
    30% {
        transform: translateY(-8px);
    }
}

/* 输入区域 */
.input-area {
    padding: 16px 24px 24px;
    background: var(--bg-primary);
    border-top: 1px solid var(--border-primary);
    flex-shrink: 0;
}

.input-container {
    max-width: 800px;
    margin: 0 auto;
}

.input-hint {
    margin: 8px 0 0;
    text-align: center;
    font-size: 12px;
    color: var(--text-tertiary);
}

/* Glass */
.glass {
    background: var(--glass-bg);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid var(--glass-border);
}

/* 动画 */
@keyframes pulse {
    0%,
    100% {
        transform: scale(1);
        opacity: 0.1;
    }
    50% {
        transform: scale(1.05);
        opacity: 0.15;
    }
}

.dropdown-enter-active,
.dropdown-leave-active {
    transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
    opacity: 0;
    transform: translateY(-8px);
}

.message-enter-active {
    animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 响应式 */
@media (max-width: 640px) {
    .example-grid {
        grid-template-columns: 1fr;
    }

    .empty-tips {
        flex-direction: column;
        gap: 12px;
        align-items: center;
    }

    .message-container {
        padding: 16px;
    }

    .input-area {
        padding: 12px 16px 20px;
    }
}
</style>
