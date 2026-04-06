<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserStore, useSessionStore, useKnowledgeBaseStore, useModelConfigStore } from "@/stores";
import { useTheme } from "@/composables";
import { chatApi, knowledgeBaseApi } from "@/api/modules";
import SessionSidebar from "@/components/business/SessionSidebar.vue";
import Avatar from "@/components/ui/Avatar.vue";
import ThemeToggle from "@/components/ui/ThemeToggle.vue";
import ModelSelector from "@/components/business/ModelSelector.vue";
import SettingsDialog from "@/components/business/SettingsDialog.vue";

const isSettingsVisible = ref(false);
function openSettings() {
    isSettingsVisible.value = true;
}

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const sessionStore = useSessionStore();
const kbStore = useKnowledgeBaseStore();
const modelConfigStore = useModelConfigStore();
const { isDark } = useTheme();

const sidebarCollapsed = ref(false);

// 加载初始数据
onMounted(async () => {
    // 先从 localStorage 快速恢复用户信息（避免页面闪烁）
    userStore.initFromStorage();
    // 再从后端同步最新 profile（含最新头像预签名 URL）
    userStore.fetchProfile().catch((e) => console.error('同步用户信息失败:', e));

    try {
        // 加载会话列表
        const sessionsRes = await chatApi.getSessions();
        sessionStore.setSessions(
            (sessionsRes.data.list || []).map((s) => ({
                id: String(s.id),
                title: s.title,
                updatedAt:
                    s.gmtModified || s.gmtCreate || new Date().toISOString(),
            })),
        );
    } catch (error) {
        console.error("加载会话列表失败:", error);
    }

    try {
        // 加载知识库列表
        const kbRes = await knowledgeBaseApi.getKnowledgeBases();
        kbStore.setKnowledgeBases(
            (kbRes.data.list || []).map((kb) => ({
                id: String(kb.id),
                name: kb.name,
                description: kb.description,
                documentCount: kb.docCount ?? 0,
                createdAt: kb.gmtCreate
                    ? String(kb.gmtCreate)
                    : new Date().toISOString(),
                updatedAt:
                    kb.gmtModified || kb.gmtCreate
                        ? String(kb.gmtModified || kb.gmtCreate)
                        : new Date().toISOString(),
            })),
        );
    } catch (error) {
        console.error("加载知识库列表失败:", error);
    }

    // 加载用户的自定义模型配置
    try {
        await modelConfigStore.fetchConfig();
    } catch (error) {
        console.error("加载模型配置失败:", error);
    }
});

// 切换侧边栏
function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
}

// 创建新会话
async function createNewSession() {
    try {
        const defaultKbId = kbStore.knowledgeBases.length > 0 ? kbStore.knowledgeBases[0].id : undefined;
        const response = await chatApi.createSession({ 
            title: "新对话",
            knowledgeBaseId: defaultKbId
        });
        const sessionId = String(response.data.id);
        sessionStore.addSession({
            id: sessionId,
            title: response.data.title,
            updatedAt:
                response.data.gmtModified ||
                response.data.gmtCreate ||
                new Date().toISOString(),
        });
        sessionStore.setCurrentSession(sessionId);
        router.push(`/chat/${sessionId}`);
    } catch (error) {
        console.error("创建会话失败:", error);
    }
}

// 退出登录
function logout() {
    userStore.clearAuth();
    sessionStore.clear();
    kbStore.clear();
    router.push("/login");
}

// 导航菜单
const navItems = [
    { path: "/chat", icon: "chat", label: "对话" },
    { path: "/knowledge", icon: "database", label: "知识库" },
];
</script>

<template>
    <div class="main-layout" :class="{ 'is-dark': isDark }">
        <!-- 侧边栏 -->
        <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
            <!-- Logo -->
            <div class="sidebar-header">
                <router-link to="/" class="logo">
                    <div class="logo-icon">
                        <svg viewBox="0 0 32 32" fill="none">
                            <path
                                d="M16 2L28 9V23L16 30L4 23V9L16 2Z"
                                fill="url(#mainLogoGradient)"
                            />
                            <path
                                d="M16 8L22 11.5V18.5L16 22L10 18.5V11.5L16 8Z"
                                fill="var(--bg-primary)"
                            />
                            <defs>
                                <linearGradient
                                    id="mainLogoGradient"
                                    x1="4"
                                    y1="2"
                                    x2="28"
                                    y2="30"
                                >
                                    <stop offset="0%" stop-color="#7c3aed" />
                                    <stop offset="100%" stop-color="#3b82f6" />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>
                    <span v-if="!sidebarCollapsed" class="logo-text"
                        >Guide<span class="highlight">RAG</span></span
                    >
                </router-link>
                <button class="toggle-btn" @click="toggleSidebar">
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                    >
                        <path v-if="sidebarCollapsed" d="M9 18l6-6-6-6" />
                        <path v-else d="M15 18l-6-6 6-6" />
                    </svg>
                </button>
            </div>

            <!-- 新建对话按钮 -->
            <button class="new-chat-btn" @click="createNewSession">
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                >
                    <path d="M12 5v14m-7-7h14" />
                </svg>
                <span v-if="!sidebarCollapsed">新建对话</span>
            </button>

            <!-- 导航菜单 -->
            <nav class="sidebar-nav">
                <router-link
                    v-for="item in navItems"
                    :key="item.path"
                    :to="item.path"
                    class="nav-item"
                    :class="{ active: route.path.startsWith(item.path) }"
                >
                    <svg
                        v-if="item.icon === 'chat'"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                    >
                        <path
                            d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
                        />
                    </svg>
                    <svg
                        v-else-if="item.icon === 'database'"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                    >
                        <ellipse cx="12" cy="5" rx="9" ry="3" />
                        <path d="M3 5V19A9 3 0 0 0 21 19V5" />
                        <path d="M3 12A9 3 0 0 0 21 12" />
                    </svg>
                    <span v-if="!sidebarCollapsed">{{ item.label }}</span>
                </router-link>
            </nav>

            <!-- 会话列表 -->
            <SessionSidebar
                v-if="route.path.startsWith('/chat') && !sidebarCollapsed"
                class="session-list"
            />

            <!-- 底部操作区 -->
            <div class="sidebar-footer">
                <!-- 主题切换 -->
                <div class="theme-toggle-wrapper">
                    <ThemeToggle mode="cycle" />
                </div>
                <!-- 底部导航菜单 -->
                <div class="footer-nav">
                    <button class="nav-item small" title="设置" @click="openSettings">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="3" />
                            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
                        </svg>
                        <span v-if="!sidebarCollapsed">设置</span>
                    </button>

                    <!-- 模型选择器 -->
                    <ModelSelector :is-collapsed="sidebarCollapsed" />

                    <button class="nav-item small danger" @click="logout" title="退出登录">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                            <polyline points="16 17 21 12 16 7" />
                            <line x1="21" y1="12" x2="9" y2="12" />
                        </svg>
                        <span v-if="!sidebarCollapsed">退出登录</span>
                    </button>
                </div>
            </div>
        </aside>

        <!-- 主内容区 -->
        <main class="main-content">
            <!-- 顶部账号信息 -->
            <header class="main-header glass">
                <div class="header-left"></div>
                <div class="header-right">
                    <div class="top-user-info animate-fade-in" @click="openSettings">
                        <Avatar 
                            :name="userStore.userName" 
                            :src="userStore.userAvatar"
                            size="sm" 
                        />
                        <span class="top-user-name">{{ userStore.userName }}</span>
                    </div>
                </div>
            </header>
            <SettingsDialog v-model:visible="isSettingsVisible" />
            <div class="view-content">
                <router-view />
            </div>
        </main>
    </div>
</template>

<style scoped>
.main-layout {
    display: flex;
    height: 100vh;
    overflow: hidden;
    background: var(--bg-primary);
}

/* 侧边栏 */
.sidebar {
    width: 280px;
    display: flex;
    flex-direction: column;
    background: var(--bg-secondary);
    border-right: 1px solid var(--border-primary);
    transition: width 0.3s ease;
    flex-shrink: 0;
}

.sidebar.collapsed {
    width: 72px;
}

.sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem;
    border-bottom: 1px solid var(--border-primary);
}

.logo {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    text-decoration: none;
}

.logo-icon {
    width: 36px;
    height: 36px;
    flex-shrink: 0;
}

.logo-icon svg {
    width: 100%;
    height: 100%;
}

.logo-text {
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--text-primary);
}

.logo-text .highlight {
    background: var(--brand-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.icon-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
}

.toggle-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    padding: 0;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: var(--text-tertiary);
    cursor: pointer;
    transition: all 0.2s;
}

.toggle-btn:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.toggle-btn svg {
    width: 18px;
    height: 18px;
}

/* 新建对话按钮 */
.new-chat-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    margin: 1rem;
    padding: 0.75rem;
    background: var(--brand-gradient);
    border: none;
    border-radius: 10px;
    color: white;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.new-chat-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 20px rgba(124, 58, 237, 0.4);
}

.new-chat-btn svg {
    width: 20px;
    height: 20px;
}

.collapsed .new-chat-btn {
    padding: 0.75rem;
}

/* 导航 */
.sidebar-nav {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.5rem;
}

.nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 0.625rem 1rem;
    border-radius: 8px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.2s;
}

.nav-item:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.nav-item svg {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
}

.nav-item.active {
    background: rgba(124, 58, 237, 0.1);
    color: var(--brand-primary);
}

.collapsed .nav-item {
    justify-content: center;
    padding: 0.75rem;
}

/* 会话列表适配滚动 */
.session-list {
    flex: 1;
    overflow-y: auto;
    border-top: 1px solid var(--border-primary);
    margin-top: 0.5rem;
}

.sidebar-footer {
    display: flex;
    flex-direction: column;
    padding: 0.5rem 0;
    background: var(--bg-secondary);
    gap: 0.125rem;
}

.footer-nav {
    display: flex;
    flex-direction: column;
    gap: 0.125rem;
}

.nav-item.small {
    padding: 0.625rem 1rem;
    font-size: 0.9rem;
    background: transparent;
    border: none;
    cursor: pointer;
    width: 100%;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    gap: 12px;
}

.nav-item.small:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.nav-item.small svg {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
}

.nav-item.danger:hover {
    background: rgba(239, 68, 68, 0.08);
    color: var(--error);
}

.theme-toggle-wrapper {
    display: flex;
    justify-content: center;
    padding: 0.5rem 0;
}

/* 主 Header（右上角区域） */
.main-header {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    background: var(--glass-bg);
    border-bottom: 1px solid var(--border-primary);
    flex-shrink: 0;
    z-index: 10;
}

.header-right {
    display: flex;
    align-items: center;
}

.model-trigger {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 0.625rem 1rem;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: all 0.2s;
    color: var(--text-secondary);
}

.model-trigger:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.top-user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 6px 12px;
    background: var(--bg-hover);
    border-radius: 20px;
    border: 1px solid var(--border-primary);
    cursor: pointer;
    transition: all 0.2s;
}

.top-user-info:hover {
    background: var(--bg-active);
    border-color: var(--border-secondary);
}

.top-user-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
}

.view-content {
    flex: 1;
    overflow: hidden;
}

/* 主内容 */
.main-content {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    background: var(--bg-primary);
}

/* 响应式 */
@media (max-width: 768px) {
    .sidebar {
        position: fixed;
        left: 0;
        top: 0;
        bottom: 0;
        z-index: 100;
        transform: translateX(-100%);
        transition: transform 0.3s ease;
    }

    .sidebar:not(.collapsed) {
        transform: translateX(0);
    }
}
</style>
