<script setup lang="ts">
</script>

<template>
  <div class="auth-layout">
    <div class="auth-bg">
      <div class="auth-gradient"></div>
      <div class="auth-grid"></div>
    </div>
    
    <div class="auth-container">
      <!-- Logo -->
      <router-link to="/" class="auth-logo">
        <svg class="logo-icon" viewBox="0 0 40 40" fill="none">
          <defs>
            <linearGradient id="authLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#7c3aed" />
              <stop offset="100%" style="stop-color:#2563eb" />
            </linearGradient>
          </defs>
          <circle cx="20" cy="20" r="18" fill="url(#authLogoGradient)" opacity="0.1"/>
          <path d="M12 20 L20 12 L28 20 L20 28 Z" fill="url(#authLogoGradient)"/>
          <circle cx="20" cy="20" r="4" fill="white"/>
        </svg>
        <span class="logo-text">Guide RAG</span>
      </router-link>

      <!-- 内容插槽 -->
      <div class="auth-card">
        <router-view />
      </div>

      <!-- 底部信息 -->
      <p class="auth-footer">
        © 2026 Guide RAG. All rights reserved.
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.auth-gradient {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 30%, rgba(124, 58, 237, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 70% 70%, rgba(37, 99, 235, 0.08) 0%, transparent 50%);
}

.auth-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(var(--border-primary) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-primary) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 40%, transparent 100%);
  opacity: 0.3;
}

.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  margin-bottom: 2rem;
}

.logo-icon {
  width: 48px;
  height: 48px;
}

.logo-text {
  font-size: 1.75rem;
  font-weight: 700;
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-card {
  width: 100%;
  padding: 2rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
}

.auth-footer {
  margin-top: 2rem;
  font-size: 0.85rem;
  color: var(--text-tertiary);
}
</style>
