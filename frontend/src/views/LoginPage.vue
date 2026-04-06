<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores";
import { useTheme } from "@/composables/useTheme";
import { authApi } from "@/api/modules";
import Button from "@/components/ui/Button.vue";
import Input from "@/components/ui/Input.vue";
import ThemeToggle from "@/components/ui/ThemeToggle.vue";

const router = useRouter();
const userStore = useUserStore();
const { isDark } = useTheme();

// 表单模式
const mode = ref<"login" | "register">("login");

// 表单数据
const formData = ref({
    username: "",
    password: "",
    confirmPassword: "",
    email: "",
    nickname: "",
    captchaCode: "",
});

// 验证码数据
const captcha = ref({
    uuid: "",
    base64Image: "",
});

// 表单错误
const errors = ref({
    username: "",
    password: "",
    confirmPassword: "",
    email: "",
    captcha: "",
    general: "",
});

const loading = ref(false);

// 背景动画
const mouseX = ref(0);
const mouseY = ref(0);

function handleMouseMove(e: MouseEvent) {
    mouseX.value = (e.clientX / window.innerWidth) * 100;
    mouseY.value = (e.clientY / window.innerHeight) * 100;
}

onMounted(() => {
    window.addEventListener("mousemove", handleMouseMove);
    // 加载验证码
    loadCaptcha();
});

onUnmounted(() => {
    window.removeEventListener("mousemove", handleMouseMove);
});

// 加载验证码
async function loadCaptcha() {
    try {
        const response = await authApi.getCaptcha();
        captcha.value.uuid = response.data.uuid;
        captcha.value.base64Image = response.data.base64Image;
    } catch (error: any) {
        console.error("Failed to load captcha:", error);
    }
}

// 验证表单
function validate(): boolean {
    errors.value = {
        username: "",
        password: "",
        confirmPassword: "",
        email: "",
        captcha: "",
        general: "",
    };

    let valid = true;

    if (!formData.value.username.trim()) {
        errors.value.username =
            mode.value === "login" ? "请输入邮箱" : "请输入用户名";
        valid = false;
    } else if (mode.value === "login") {
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.username)) {
            errors.value.username = "邮箱格式不正确";
            valid = false;
        }
    } else if (formData.value.username.length < 3) {
        errors.value.username = "用户名至少3个字符";
        valid = false;
    }

    if (!formData.value.password) {
        errors.value.password = "请输入密码";
        valid = false;
    } else if (formData.value.password.length < 6) {
        errors.value.password = "密码至少6个字符";
        valid = false;
    }

    if (mode.value === "register") {
        if (formData.value.password !== formData.value.confirmPassword) {
            errors.value.confirmPassword = "两次密码输入不一致";
            valid = false;
        }

        if (!formData.value.email.trim()) {
            errors.value.email = "请输入邮箱";
            valid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
            errors.value.email = "邮箱格式不正确";
            valid = false;
        }
    }

    // 登录模式也需要验证码
    if (!formData.value.captchaCode.trim()) {
        errors.value.captcha = "请输入验证码";
        valid = false;
    }

    return valid;
}

// 提交表单
async function handleSubmit() {
    if (!validate()) return;

    loading.value = true;
    errors.value.general = "";

    try {
        if (mode.value === "login") {
            const response = await authApi.login({
                email: formData.value.username, // 后端需要email字段
                password: formData.value.password,
                captchaCode: formData.value.captchaCode,
                captchaUuid: captcha.value.uuid,
            });

            // 后端返回的是字符串token
            const token = response.data;

            // 解析JWT获取用户信息（简单解析，实际应该用库）
            const payload = JSON.parse(atob(token.split(".")[1]));

            const userData = {
                id: payload.sub,
                username: payload.username || formData.value.username,
                email: formData.value.username,
                nickname: payload.username || formData.value.username,
                createdAt: new Date().toISOString(),
            };

            userStore.setAuth(token, userData);
            router.push("/chat");
        } else {
            const response = await authApi.register({
                username: formData.value.username,
                password: formData.value.password,
                email: formData.value.email,
                confirmPassword: formData.value.password,
                captchaCode: formData.value.captchaCode,
                captchaUuid: captcha.value.uuid,
            });

            // 注册成功，后端已直接返回 JWT Token，无需再发起二次登录
            const token = response.data;
            const payload = JSON.parse(atob(token.split(".")[1]));

            const userData = {
                id: payload.sub,
                username: payload.username || formData.value.username,
                email: formData.value.email,
                nickname:
                    formData.value.nickname ||
                    payload.username ||
                    formData.value.username,
                createdAt: new Date().toISOString(),
            };

            userStore.setAuth(token, userData);
            router.push("/chat");
        }
    } catch (error: any) {
        errors.value.general = error.message || "网络错误，请稍后重试";
        // 登录失败后刷新验证码
        loadCaptcha();
        formData.value.captchaCode = "";
    } finally {
        loading.value = false;
    }
}

// 切换模式
function switchMode() {
    mode.value = mode.value === "login" ? "register" : "login";
    errors.value = {
        username: "",
        password: "",
        confirmPassword: "",
        email: "",
        captcha: "",
        general: "",
    };
    formData.value.captchaCode = "";
    loadCaptcha();
}


function goHome() {
    router.push("/");
}
</script>

<template>
    <div class="login-page" :class="{ 'is-dark': isDark }">
        <!-- 背景装饰 -->
        <div class="bg-decoration">
            <div
                class="gradient-orb orb-1"
                :style="{
                    transform: `translate(${mouseX * 0.02}px, ${mouseY * 0.02}px)`,
                }"
            ></div>
            <div
                class="gradient-orb orb-2"
                :style="{
                    transform: `translate(${-mouseX * 0.015}px, ${-mouseY * 0.015}px)`,
                }"
            ></div>
            <div class="grid-pattern"></div>
        </div>

        <!-- 导航 -->
        <nav class="navbar">
            <button class="logo" @click="goHome">
                <div class="logo-icon">
                    <svg viewBox="0 0 32 32" fill="none">
                        <path
                            d="M16 2L28 9V23L16 30L4 23V9L16 2Z"
                            fill="url(#grad1)"
                        />
                        <path
                            d="M16 8L22 11.5V18.5L16 22L10 18.5V11.5L16 8Z"
                            fill="var(--bg-primary)"
                        />
                        <defs>
                            <linearGradient
                                id="grad1"
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
                <span class="logo-text">Guide<span class="highlight">RAG</span></span>
            </button>
            <ThemeToggle />
        </nav>

        <!-- 主内容 -->
        <div class="content">
            <!-- 左侧装饰 -->
            <div class="side-decoration">
                <div class="decoration-card glass">
                    <div class="card-icon">
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <path
                                d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"
                            />
                        </svg>
                    </div>
                    <h3>智能对话</h3>
                    <p>基于 RAG 技术的智能问答，精准检索知识库内容</p>
                </div>

                <div class="decoration-card glass">
                    <div class="card-icon">
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
                    </div>
                    <h3>安全可控</h3>
                    <p>企业级安全架构，数据隔离、权限控制、审计追踪</p>
                </div>

                <div class="decoration-card glass">
                    <div class="card-icon">
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                        >
                            <polygon
                                points="13,2 3,14 12,14 11,22 21,10 12,10"
                            />
                        </svg>
                    </div>
                    <h3>极速响应</h3>
                    <p>流式输出，毫秒级响应，打字机效果丝滑流畅</p>
                </div>
            </div>

            <!-- 登录表单 -->
            <div class="form-container">
                <div class="form-card glass">
                    <div class="form-header">
                        <h1 class="title">
                            {{ mode === "login" ? "欢迎回来" : "创建账号" }}
                        </h1>
                        <p class="subtitle">
                            {{
                                mode === "login"
                                    ? "登录以继续使用 GuideRAG"
                                    : "注册一个新账号开始使用"
                            }}
                        </p>
                    </div>

                    <form class="form" @submit.prevent="handleSubmit">
                        <!-- 通用错误 -->
                        <Transition name="fade">
                            <div v-if="errors.general" class="error-alert">
                                <svg
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                >
                                    <circle cx="12" cy="12" r="10" />
                                    <line x1="12" y1="8" x2="12" y2="12" />
                                    <line x1="12" y1="16" x2="12.01" y2="16" />
                                </svg>
                                {{ errors.general }}
                            </div>
                        </Transition>

                        <!-- 用户名 -->
                        <div class="form-group">
                            <label class="label">
                                <svg
                                    v-if="mode === 'login'"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                >
                                    <path
                                        d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"
                                    />
                                    <polyline points="22,6 12,13 2,6" />
                                </svg>
                                <svg
                                    v-else
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                >
                                    <path
                                        d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"
                                    />
                                    <circle cx="12" cy="7" r="4" />
                                </svg>
                                {{ mode === "login" ? "邮箱" : "用户名" }}
                            </label>
                            <Input
                                v-model="formData.username"
                                :placeholder="
                                    mode === 'login' ? '请输入邮箱' : '请输入用户名'
                                "
                                :error="errors.username"
                            />
                        </div>

                        <!-- 邮箱（仅注册） -->
                        <Transition name="slide">
                            <div v-if="mode === 'register'" class="form-group">
                                <label class="label">
                                    <svg
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        stroke-width="2"
                                    >
                                        <path
                                            d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"
                                        />
                                        <polyline points="22,6 12,13 2,6" />
                                    </svg>
                                    邮箱
                                </label>
                                <Input
                                    v-model="formData.email"
                                    type="email"
                                    placeholder="请输入邮箱"
                                    :error="errors.email"
                                />
                            </div>
                        </Transition>

                        <!-- 昵称（仅注册，可选） -->
                        <Transition name="slide">
                            <div v-if="mode === 'register'" class="form-group">
                                <label class="label">
                                    <svg
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        stroke-width="2"
                                    >
                                        <circle cx="12" cy="12" r="10" />
                                        <path d="M8 14s1.5 2 4 2 4-2 4-2" />
                                        <line x1="9" y1="9" x2="9.01" y2="9" />
                                        <line
                                            x1="15"
                                            y1="9"
                                            x2="15.01"
                                            y2="9"
                                        />
                                    </svg>
                                    昵称 <span class="optional">（可选）</span>
                                </label>
                                <Input
                                    v-model="formData.nickname"
                                    placeholder="请输入昵称"
                                />
                            </div>
                        </Transition>

                        <!-- 密码 -->
                        <div class="form-group">
                            <label class="label">
                                <svg
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                >
                                    <rect
                                        x="3"
                                        y="11"
                                        width="18"
                                        height="11"
                                        rx="2"
                                        ry="2"
                                    />
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                </svg>
                                密码
                            </label>
                            <Input
                                v-model="formData.password"
                                type="password"
                                placeholder="请输入密码"
                                :error="errors.password"
                            />
                        </div>

                        <!-- 确认密码（仅注册） -->
                        <Transition name="slide">
                            <div v-if="mode === 'register'" class="form-group">
                                <label class="label">
                                    <svg
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        stroke-width="2"
                                    >
                                        <path
                                            d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"
                                        />
                                        <path d="M9 12l2 2 4-4" />
                                    </svg>
                                    确认密码
                                </label>
                                <Input
                                    v-model="formData.confirmPassword"
                                    type="password"
                                    placeholder="请再次输入密码"
                                    :error="errors.confirmPassword"
                                />
                            </div>
                        </Transition>

                        <!-- 验证码 -->
                        <div class="form-group">
                            <label class="label">
                                <svg
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    stroke-width="2"
                                >
                                    <rect
                                        x="3"
                                        y="3"
                                        width="18"
                                        height="18"
                                        rx="2"
                                        ry="2"
                                    />
                                    <path d="M9 9h.01M15 9h.01M9 15h6" />
                                </svg>
                                验证码
                            </label>
                            <div class="captcha-group">
                                <Input
                                    v-model="formData.captchaCode"
                                    placeholder="请输入验证码"
                                    :error="errors.captcha"
                                    style="flex: 1"
                                />
                                <img
                                    v-if="captcha.base64Image"
                                    :src="captcha.base64Image"
                                    alt="验证码"
                                    class="captcha-image"
                                    @click="loadCaptcha"
                                    title="点击刷新验证码"
                                />
                            </div>
                        </div>

                        <!-- 提交按钮 -->
                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            block
                            :loading="loading"
                        >
                            {{ mode === "login" ? "登录" : "注册" }}
                        </Button>

                    </form>

                    <!-- 切换模式 -->
                    <p class="switch-mode">
                        {{ mode === "login" ? "还没有账号？" : "已有账号？" }}
                        <button
                            type="button"
                            class="switch-btn"
                            @click="switchMode"
                        >
                            {{ mode === "login" ? "立即注册" : "立即登录" }}
                        </button>
                    </p>
                </div>
            </div>
        </div>

        <!-- 底部 -->
        <footer class="footer">
            <p>© 2026 GuideRAG. All rights reserved.</p>
        </footer>
    </div>
</template>

<style scoped>
.login-page {
    position: relative;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background: var(--bg-primary);
}

/* 背景装饰 */
.bg-decoration {
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 0;
}

.gradient-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(100px);
    opacity: 0.4;
    transition: transform 0.3s ease-out;
}

.orb-1 {
    width: 500px;
    height: 500px;
    background: linear-gradient(135deg, #7c3aed, #3b82f6);
    top: -100px;
    right: -100px;
}

.orb-2 {
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, #ec4899, #8b5cf6);
    bottom: -100px;
    left: -100px;
}

.grid-pattern {
    position: absolute;
    inset: 0;
    background-image:
        linear-gradient(var(--border-primary) 1px, transparent 1px),
        linear-gradient(90deg, var(--border-primary) 1px, transparent 1px);
    background-size: 50px 50px;
    opacity: 0.3;
}

/* 导航 */
.navbar {
    position: relative;
    z-index: 10;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 40px;
}

.logo {
    display: flex;
    align-items: center;
    gap: 10px;
    background: transparent;
    border: none;
    cursor: pointer;
}

.logo-icon {
    width: 36px;
    height: 36px;
}

.logo-icon svg {
    width: 100%;
    height: 100%;
}

.logo-text {
    font-size: 22px;
    font-weight: 700;
    color: var(--text-primary);
}

.logo-text .highlight {
    background: var(--brand-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

/* 内容区 */
.content {
    position: relative;
    z-index: 1;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 80px;
    padding: 40px;
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
}

/* 左侧装饰 */
.side-decoration {
    display: flex;
    flex-direction: column;
    gap: 20px;
    max-width: 360px;
}

.decoration-card {
    padding: 24px;
    border-radius: 16px;
    transition: transform 0.3s ease;
}

.decoration-card:hover {
    transform: translateX(8px);
}

.card-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--brand-gradient);
    border-radius: 12px;
    margin-bottom: 16px;
}

.card-icon svg {
    width: 24px;
    height: 24px;
    color: white;
}

.decoration-card h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
}

.decoration-card p {
    font-size: 14px;
    line-height: 1.6;
    color: var(--text-secondary);
    margin: 0;
}

/* 表单容器 */
.form-container {
    width: 100%;
    max-width: 420px;
}

.form-card {
    padding: 40px;
    border-radius: 24px;
    animation: fadeInUp 0.6s ease-out;
}

.form-header {
    text-align: center;
    margin-bottom: 32px;
}

.title {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
}

.subtitle {
    margin: 0;
    font-size: 15px;
    color: var(--text-secondary);
}

/* 表单 */
.form {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-secondary);
}

.label svg {
    width: 16px;
    height: 16px;
    opacity: 0.7;
}

.optional {
    font-weight: 400;
    color: var(--text-tertiary);
}

.captcha-group {
    display: flex;
    gap: 12px;
    align-items: flex-start;
}

.captcha-image {
    height: 48px;
    border-radius: 8px;
    border: 1px solid var(--border-primary);
    cursor: pointer;
    transition: all 0.2s;
    flex-shrink: 0;
}

.captcha-image:hover {
    border-color: var(--brand-primary);
    transform: scale(1.02);
}

.error-alert {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    background: var(--error-bg);
    border: 1px solid var(--error);
    border-radius: 12px;
    color: var(--error);
    font-size: 14px;
}

.error-alert svg {
    width: 18px;
    height: 18px;
    flex-shrink: 0;
}

.divider {
    display: flex;
    align-items: center;
    gap: 16px;
    color: var(--text-tertiary);
    font-size: 13px;
}

.divider::before,
.divider::after {
    content: "";
    flex: 1;
    height: 1px;
    background: var(--border-primary);
}

.switch-mode {
    margin-top: 24px;
    font-size: 14px;
    color: var(--text-tertiary);
    text-align: center;
}

.switch-btn {
    padding: 0;
    background: none;
    border: none;
    color: var(--brand-primary);
    font-size: inherit;
    font-weight: 500;
    cursor: pointer;
    transition: color 0.2s;
}

.switch-btn:hover {
    color: var(--brand-secondary);
}

/* 底部 */
.footer {
    position: relative;
    z-index: 1;
    padding: 20px;
    text-align: center;
    font-size: 13px;
    color: var(--text-tertiary);
}

/* Glass */
.glass {
    background: var(--glass-bg);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid var(--glass-border);
}

/* 动画 */
@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
    transition: all 0.3s ease;
}

.slide-enter-from {
    opacity: 0;
    transform: translateY(-10px);
}

.slide-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}

/* 响应式 */
@media (max-width: 900px) {
    .content {
        flex-direction: column;
        gap: 40px;
    }

    .side-decoration {
        display: none;
    }

    .form-card {
        padding: 32px 24px;
    }
}

@media (max-width: 480px) {
    .navbar {
        padding: 16px 20px;
    }

    .content {
        padding: 20px;
    }

    .form-card {
        padding: 24px 20px;
    }

    .title {
        font-size: 24px;
    }
}
</style>
