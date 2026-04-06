<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { isDark } = useTheme()
const userStore = useUserStore()

const isScrolled = ref(false)
const isMenuOpen = ref(false)

// 打字机效果
const phrases = ['企业知识管理', '智能问答系统', '文档检索增强', '私有化部署']
const currentPhraseIndex = ref(0)
const currentText = ref('')
const isDeleting = ref(false)

let typingTimeout: number | null = null

function typeEffect() {
  const currentPhrase = phrases[currentPhraseIndex.value]
  
  if (isDeleting.value) {
    currentText.value = currentPhrase.substring(0, currentText.value.length - 1)
    if (currentText.value === '') {
      isDeleting.value = false
      currentPhraseIndex.value = (currentPhraseIndex.value + 1) % phrases.length
    }
  } else {
    currentText.value = currentPhrase.substring(0, currentText.value.length + 1)
    if (currentText.value === currentPhrase) {
      isDeleting.value = true
      typingTimeout = window.setTimeout(typeEffect, 2000)
      return
    }
  }
  
  typingTimeout = window.setTimeout(typeEffect, isDeleting.value ? 50 : 100)
}

onMounted(() => {
  typeEffect()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  if (typingTimeout) clearTimeout(typingTimeout)
  window.removeEventListener('scroll', handleScroll)
})

function handleScroll() {
  isScrolled.value = window.scrollY > 20
}

// 统计数据
const stats = [
  { value: '99.9%', label: '系统可靠性' },
  { value: '<100ms', label: '毫秒级响应' },
  { value: '集成度', label: '零配置上手' },
  { value: '私有化', label: '数据全加密' }
]

// 核心功能
const features = [
  {
    icon: 'search',
    title: '语义检索',
    description: '基于向量相似度的智能检索，理解用户真实意图，突破关键词匹配的局限',
    gradient: 'from-violet-500 to-purple-600'
  },
  {
    icon: 'chat',
    title: '对话式交互',
    description: '自然语言交互，支持多轮对话上下文理解，提供流畅的问答体验',
    gradient: 'from-blue-500 to-cyan-500'
  },
  {
    icon: 'doc',
    title: '多格式支持',
    description: '支持 PDF、Word、Markdown、TXT 等主流文档格式，自动解析提取',
    gradient: 'from-emerald-500 to-teal-500'
  },
  {
    icon: 'shield',
    title: '企业级安全',
    description: '完善的权限控制、数据隔离、审计日志，满足企业合规需求',
    gradient: 'from-orange-500 to-amber-500'
  },
  {
    icon: 'speed',
    title: '流式响应',
    description: '基于 SSE 的实时流式输出，打字机效果呈现，体验丝滑流畅',
    gradient: 'from-pink-500 to-rose-500'
  },
  {
    icon: 'api',
    title: '开放集成',
    description: '完整的 RESTful API，轻松集成到现有系统，快速赋能业务智能化',
    gradient: 'from-indigo-500 to-blue-600'
  }
]

// 应用场景
const scenarios = [
  {
    title: '个人知识大脑',
    description: '整理散落在笔记、公号收藏和本地文件里的知识，构建你随时可调用的第二大脑。',
    icon: 'brain'
  },
  {
    title: '学术科研助手',
    description: '一次性上传数十本专业书籍或 PDF 论文，快速总结核心论点并寻找相关引用。',
    icon: 'book'
  },
  {
    title: '深度代码专家',
    description: '导入项目所有的工程文档和 API 参考，即便没有 Readme 也能轻松掌握技术细节。',
    icon: 'code'
  }
]

// 导航
function goToLogin() {
  router.push('/login')
}

function goToChat() {
  router.push('/chat')
}

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  isMenuOpen.value = false
}
</script>

<template>
  <div class="landing" :class="{ 'is-dark': isDark }">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <div class="grid-pattern"></div>
    </div>

    <!-- 导航栏 -->
    <header class="navbar" :class="{ scrolled: isScrolled }">
      <div class="navbar-container">
        <div class="navbar-brand">
          <div class="logo">
            <div class="logo-icon">
              <svg viewBox="0 0 32 32" fill="none">
                <path d="M16 2L28 9V23L16 30L4 23V9L16 2Z" fill="url(#grad1)" />
                <path d="M16 8L22 11.5V18.5L16 22L10 18.5V11.5L16 8Z" fill="var(--bg-primary)" />
                <defs>
                  <linearGradient id="grad1" x1="4" y1="2" x2="28" y2="30">
                    <stop offset="0%" stop-color="#7c3aed" />
                    <stop offset="100%" stop-color="#3b82f6" />
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <span class="logo-text">Guide<span class="highlight">RAG</span></span>
          </div>
        </div>

        <nav class="navbar-menu" :class="{ open: isMenuOpen }">
          <a href="#features" @click.prevent="scrollToSection('features')">核心功能</a>
          <a href="#architecture" @click.prevent="scrollToSection('architecture')">技术栈</a>
          <a href="#scenarios" @click.prevent="scrollToSection('scenarios')">应用场景</a>
        </nav>

        <div class="navbar-actions">
          <ThemeToggle />
          <template v-if="!userStore.isLoggedIn">
            <button class="btn-ghost" @click="goToLogin">登录</button>
          </template>
        </div>

        <button class="mobile-menu-btn" @click="isMenuOpen = !isMenuOpen">
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </header>

    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-container">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span>全新 2.0 版本发布</span>
        </div>
        
        <h1 class="hero-title">
          下一代
          <span class="gradient-text">AI 知识库</span>
          <br />
          解决方案
        </h1>
        
        <div class="hero-typing">
          <span>用于</span>
          <span class="typing-text">{{ currentText }}</span>
          <span class="cursor">|</span>
        </div>
        
        <p class="hero-description">
          基于检索增强生成（RAG）技术，将你的个人文档转化为智能知识库。
          <br />
          精准检索、私密存储、自然对话，让阅读和学习更高效。
        </p>
        
        <div class="hero-actions">
          <button class="btn-primary btn-lg" @click="goToChat">
            <span>{{ userStore.isLoggedIn ? '继续对话' : '开始体验' }}</span>
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
        
        <!-- Stats -->
        <div class="hero-stats">
          <div class="stat-item" v-for="stat in stats" :key="stat.label">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <!-- Hero Visual -->
      <div class="hero-visual">
        <div class="visual-card glass">
          <div class="chat-demo">
            <div class="chat-header">
              <div class="dots">
                <span></span><span></span><span></span>
              </div>
              <span>RAG 智能助手</span>
            </div>
            <div class="chat-messages">
              <div class="message user">
                <div class="message-content">如何部署微服务架构？</div>
              </div>
              <div class="message assistant">
                <div class="message-content">
                  <p>根据您的知识库文档，微服务部署推荐以下方案：</p>
                  <ol>
                    <li>使用 <strong>Docker</strong> 容器化各服务</li>
                    <li>通过 <strong>Kubernetes</strong> 进行编排</li>
                    <li>配置 <strong>Service Mesh</strong> 实现服务治理</li>
                  </ol>
                  <div class="source-tag">📚 来源: 微服务架构指南.pdf</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="features">
      <div class="section-container">
        <div class="section-header">
          <span class="section-tag">功能特性</span>
          <h2 class="section-title">为企业级应用而生</h2>
          <p class="section-description">
            从文档解析到智能问答，提供全链路解决方案
          </p>
        </div>
        
        <div class="features-grid">
          <div 
            v-for="(feature, index) in features" 
            :key="feature.title" 
            class="feature-card"
            :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <div class="feature-icon" :class="feature.gradient">
              <!-- Search Icon -->
              <svg v-if="feature.icon === 'search'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <path d="M21 21l-4.35-4.35"/>
              </svg>
              <!-- Chat Icon -->
              <svg v-else-if="feature.icon === 'chat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
              </svg>
              <!-- Doc Icon -->
              <svg v-else-if="feature.icon === 'doc'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                <polyline points="14,2 14,8 20,8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
              <!-- Shield Icon -->
              <svg v-else-if="feature.icon === 'shield'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <!-- Speed Icon -->
              <svg v-else-if="feature.icon === 'speed'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13,2 3,14 12,14 11,22 21,10 12,10"/>
              </svg>
              <!-- API Icon -->
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="16,18 22,12 16,6"/>
                <polyline points="8,6 2,12 8,18"/>
              </svg>
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Architecture Section -->
    <section id="architecture" class="architecture">
      <div class="section-container">
        <div class="section-header">
          <span class="section-tag">技术架构</span>
          <h2 class="section-title">现代化技术栈</h2>
          <p class="section-description">
            基于业界主流技术构建，稳定可靠、易于扩展
          </p>
        </div>
        
        <div class="arch-diagram">
          <div class="arch-layer">
            <div class="layer-label">前端层</div>
            <div class="tech-badges">
              <span class="tech-badge vue">Vue 3</span>
              <span class="tech-badge ts">TypeScript</span>
              <span class="tech-badge vite">Vite</span>
            </div>
          </div>
          
          <div class="arch-arrow">↓</div>
          
          <div class="arch-layer">
            <div class="layer-label">网关层</div>
            <div class="tech-badges">
              <span class="tech-badge spring">Spring Cloud Gateway</span>
              <span class="tech-badge">JWT Auth</span>
            </div>
          </div>
          
          <div class="arch-arrow">↓</div>
          
          <div class="arch-layer">
            <div class="layer-label">服务层</div>
            <div class="tech-badges">
              <span class="tech-badge spring">Spring Boot 3</span>
              <span class="tech-badge ai">LangChain4j</span>
              <span class="tech-badge">OpenAI API</span>
            </div>
          </div>
          
          <div class="arch-arrow">↓</div>
          
          <div class="arch-layer">
            <div class="layer-label">存储层</div>
            <div class="tech-badges">
              <span class="tech-badge db">PostgreSQL</span>
              <span class="tech-badge vector">Milvus</span>
              <span class="tech-badge redis">Redis</span>
              <span class="tech-badge">MinIO</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Scenarios Section -->
    <section id="scenarios" class="scenarios">
      <div class="section-container">
        <div class="section-header">
          <span class="section-tag">应用场景</span>
          <h2 class="section-title">开启你的智能生活</h2>
          <p class="section-description">
            不论是学习新领域还是管理碎片化信息，GuideRAG 都能为你提供最精准的答案
          </p>
        </div>
        
        <div class="scenarios-grid">
          <div 
            v-for="(scenario, index) in scenarios" 
            :key="index" 
            class="scenario-card glass"
            :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <div class="scenario-icon">
              <!-- Brain Icon -->
              <svg v-if="scenario.icon === 'brain'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
              <!-- Book Icon -->
              <svg v-else-if="scenario.icon === 'book'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
              </svg>
              <!-- Code Icon -->
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" xmlns="http://www.w3.org/2000/svg">
                <polyline points="16 18 22 12 16 6"/>
                <polyline points="8 6 2 12 8 18"/>
              </svg>
            </div>
            <h3 class="scenario-title">{{ scenario.title }}</h3>
            <p class="scenario-description">{{ scenario.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta">
      <div class="cta-container glass">
        <h2 class="cta-title">准备好开始了吗？</h2>
        <p class="cta-description">立即体验 GuideRAG，让您的知识库智能起来</p>
        <div class="cta-actions">
          <button class="btn-primary btn-lg" @click="goToChat">
            {{ userStore.isLoggedIn ? '进入我的智库' : '开始体验' }}
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-container">
        <div class="footer-brand">
          <div class="logo">
            <div class="logo-icon">
              <svg viewBox="0 0 32 32" fill="none">
                <path d="M16 2L28 9V23L16 30L4 23V9L16 2Z" fill="url(#grad2)" />
                <path d="M16 8L22 11.5V18.5L16 22L10 18.5V11.5L16 8Z" fill="var(--bg-primary)" />
                <defs>
                  <linearGradient id="grad2" x1="4" y1="2" x2="28" y2="30">
                    <stop offset="0%" stop-color="#7c3aed" />
                    <stop offset="100%" stop-color="#3b82f6" />
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <span class="logo-text">Guide<span class="highlight">RAG</span></span>
          </div>
          <p class="footer-tagline">你的个人 AI 智能知识管家</p>
        </div>
        
        <div class="footer-links">
          <div class="link-group">
            <h4>产品</h4>
            <a href="#">功能特性</a>
            <a href="#">技术架构</a>
            <a href="#">更新日志</a>
          </div>
          <div class="link-group">
            <h4>资源</h4>
            <a href="#">文档中心</a>
            <a href="#">API 参考</a>
            <a href="#">开发者指南</a>
            <a href="#">示例代码</a>
          </div>
          <div class="link-group">
            <h4>公司</h4>
            <a href="#">关于我们</a>
            <a href="#">联系我们</a>
            <a href="#">加入我们</a>
            <a href="#">合作伙伴</a>
          </div>
        </div>
      </div>
      
      <div class="footer-bottom">
        <p>© 2026 GuideRAG. All rights reserved.</p>
        <div class="footer-legal">
          <a href="#">隐私政策</a>
          <a href="#">服务条款</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ========================================
   Landing Page Styles
   ======================================== */

.landing {
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
}

/* Background Decoration */
.bg-decoration {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #7c3aed 0%, #3b82f6 100%);
  top: -200px;
  right: -200px;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  bottom: 20%;
  left: -150px;
}

.orb-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%);
  top: 50%;
  right: 10%;
  opacity: 0.3;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(var(--border-primary) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-primary) 1px, transparent 1px);
  background-size: 60px 60px;
  opacity: 0.3;
}

/* Navbar */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 16px 24px;
  transition: all 0.3s ease;
  background: transparent;
}

.navbar.scrolled {
  background: var(--glass-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--glass-border);
  padding: 12px 24px;
  box-shadow: 0 4px 20px -5px rgba(0, 0, 0, 0.1);
}

.navbar-container {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
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

.navbar-menu {
  display: flex;
  gap: 32px;
}

.navbar-menu a {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.navbar-menu a:hover {
  color: var(--text-primary);
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 48px; /* 显式限制容器高度区域 */
}

/* Buttons */
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  background: var(--brand-gradient);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-primary:hover {
  background: var(--brand-gradient-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.4);
}

.btn-primary svg {
  width: 16px;
  height: 16px;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-secondary);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline:hover {
  background: var(--bg-hover);
  border-color: var(--text-tertiary);
}

.btn-outline svg {
  width: 16px;
  height: 16px;
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  color: var(--text-secondary);
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-ghost:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.btn-lg {
  padding: 14px 28px;
  font-size: 16px;
}

.btn-block {
  width: 100%;
  justify-content: center;
}

.mobile-menu-btn {
  display: none;
  flex-direction: column;
  gap: 5px;
  padding: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
}

.mobile-menu-btn span {
  width: 24px;
  height: 2px;
  background: var(--text-primary);
  transition: all 0.3s ease;
}

/* Hero Section */
.hero {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140px 24px 80px;
}

.hero-container {
  max-width: 800px;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: 100px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  animation: fadeInDown 0.6s ease-out;
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.hero-title {
  font-size: clamp(40px, 8vw, 72px);
  font-weight: 800;
  line-height: 1.1;
  color: var(--text-primary);
  margin-bottom: 16px;
  animation: fadeInUp 0.6s ease-out;
}

.gradient-text {
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-typing {
  font-size: 20px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  height: 32px;
  animation: fadeInUp 0.6s ease-out 0.1s backwards;
}

.typing-text {
  color: var(--brand-primary);
  font-weight: 600;
}

.cursor {
  animation: pulse 1s infinite;
}

.hero-description {
  font-size: 18px;
  line-height: 1.7;
  color: var(--text-secondary);
  margin-bottom: 40px;
  animation: fadeInUp 0.6s ease-out 0.2s backwards;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 60px;
  animation: fadeInUp 0.6s ease-out 0.3s backwards;
}

.hero-stats {
  display: flex;
  gap: 48px;
  justify-content: center;
  animation: fadeInUp 0.6s ease-out 0.4s backwards;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-tertiary);
}

/* Hero Visual */
.hero-visual {
  margin-top: 60px;
  animation: fadeInUp 0.8s ease-out 0.5s backwards;
}

.visual-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

.chat-demo {
  width: min(600px, 90vw);
  background: var(--bg-secondary);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-primary);
  font-size: 13px;
  color: var(--text-secondary);
}

.dots {
  display: flex;
  gap: 6px;
}

.dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--border-secondary);
}

.dots span:nth-child(1) { background: #ef4444; }
.dots span:nth-child(2) { background: #f59e0b; }
.dots span:nth-child(3) { background: #10b981; }

.chat-messages {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message-content {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.message.user .message-content {
  background: var(--brand-gradient);
  color: white;
}

.message.assistant .message-content {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.message.assistant .message-content p {
  margin-bottom: 8px;
  color: var(--text-primary);
}

.message.assistant .message-content ol {
  padding-left: 20px;
  margin: 8px 0;
}

.message.assistant .message-content li {
  margin: 4px 0;
  color: var(--text-secondary);
}

.message.assistant .message-content strong {
  color: var(--brand-primary);
}

.source-tag {
  margin-top: 12px;
  padding: 6px 10px;
  background: var(--bg-hover);
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* Section Styles */
.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-tag {
  display: inline-block;
  padding: 6px 14px;
  background: var(--brand-primary);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.2), rgba(59, 130, 246, 0.2));
  color: var(--brand-primary);
  border-radius: 100px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}

.section-title {
  font-size: clamp(32px, 5vw, 48px);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.section-description {
  font-size: 18px;
  color: var(--text-secondary);
  max-width: 600px;
  margin: 0 auto;
}

/* Features Section */
.features {
  position: relative;
  z-index: 1;
  padding: 100px 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.feature-card {
  padding: 32px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.6s ease-out backwards;
}

.feature-card:hover {
  transform: translateY(-4px);
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 1px var(--brand-primary);
}

.feature-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  margin-bottom: 20px;
  background: var(--brand-gradient);
}

.feature-icon svg {
  width: 24px;
  height: 24px;
  color: white;
}

.feature-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.feature-description {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-secondary);
}

/* Architecture Section */
.architecture {
  position: relative;
  z-index: 1;
  padding: 100px 0;
  background: var(--bg-secondary);
}

.arch-diagram {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.arch-layer {
  padding: 24px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  text-align: center;
}

.layer-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.tech-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.tech-badge {
  padding: 6px 14px;
  background: var(--bg-tertiary);
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.tech-badge.vue { background: rgba(66, 184, 131, 0.15); color: #42b883; }
.tech-badge.ts { background: rgba(49, 120, 198, 0.15); color: #3178c6; }
.tech-badge.vite { background: rgba(189, 52, 254, 0.15); color: #bd34fe; }
.tech-badge.spring { background: rgba(109, 179, 63, 0.15); color: #6db33f; }
.tech-badge.ai { background: rgba(124, 58, 237, 0.15); color: #7c3aed; }
.tech-badge.db { background: rgba(51, 103, 145, 0.15); color: #336791; }
.tech-badge.vector { background: rgba(0, 184, 212, 0.15); color: #00b8d4; }
.tech-badge.redis { background: rgba(220, 56, 45, 0.15); color: #dc382d; }

.arch-arrow {
  text-align: center;
  font-size: 20px;
  color: var(--text-tertiary);
}

/* Scenarios Section */
.scenarios {
  position: relative;
  z-index: 1;
  padding: 100px 0;
}

.scenarios-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 32px;
  margin-top: 40px;
}

.scenario-card {
  padding: 48px 32px;
  border-radius: 24px;
  text-align: center;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.scenario-card:hover {
  transform: translateY(-12px);
  border-color: var(--brand-primary);
  box-shadow: 0 20px 40px -15px rgba(124, 58, 237, 0.3);
}

.scenario-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-gradient);
  border-radius: 18px;
  color: white;
  transform: rotate(-5deg);
  transition: transform 0.3s ease;
}

.scenario-card:hover .scenario-icon {
  transform: rotate(0deg) scale(1.1);
}

.scenario-icon svg {
  width: 32px;
  height: 32px;
}

.scenario-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.scenario-description {
  font-size: 16px;
  line-height: 1.7;
  color: var(--text-secondary);
}

/* CTA Section */
.cta {
  position: relative;
  z-index: 1;
  padding: 100px 24px;
}

.cta-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 60px;
  border-radius: 24px;
  text-align: center;
}

.cta-title {
  font-size: clamp(28px, 5vw, 40px);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.cta-description {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.cta-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* Footer */
.footer {
  position: relative;
  z-index: 1;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-primary);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 24px;
  display: flex;
  justify-content: space-between;
  gap: 60px;
}

.footer-brand {
  max-width: 300px;
}

.footer-tagline {
  margin-top: 12px;
  font-size: 14px;
  color: var(--text-tertiary);
}

.footer-links {
  display: flex;
  gap: 60px;
}

.link-group h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.link-group a {
  display: block;
  padding: 6px 0;
  font-size: 14px;
  color: var(--text-tertiary);
  transition: color 0.2s ease;
}

.link-group a:hover {
  color: var(--text-primary);
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  border-top: 1px solid var(--border-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--text-tertiary);
}

.footer-legal {
  display: flex;
  gap: 24px;
}

.footer-legal a {
  color: var(--text-tertiary);
}

.footer-legal a:hover {
  color: var(--text-primary);
}

/* Glass Effect */
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--glass-border);
}

/* Responsive */
@media (max-width: 768px) {
  .navbar-menu {
    display: none;
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex-direction: column;
    padding: 20px;
    background: var(--bg-secondary);
    border-bottom: 1px solid var(--border-primary);
  }

  .navbar-menu.open {
    display: flex;
  }

  .navbar-actions {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-stats {
    flex-wrap: wrap;
    gap: 24px;
  }

  .footer-container {
    flex-direction: column;
  }

  .footer-links {
    flex-wrap: wrap;
    gap: 40px;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .cta-actions {
    flex-direction: column;
  }

  .cta-container {
    padding: 40px 24px;
  }
}
</style>
