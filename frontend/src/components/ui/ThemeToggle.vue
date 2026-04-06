<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

const { theme, isDark, toggleTheme, cycleTheme } = useTheme()

interface Props {
  mode?: 'toggle' | 'cycle'
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'toggle'
})

function handleClick() {
  if (props.mode === 'cycle') {
    cycleTheme()
  } else {
    toggleTheme()
  }
}

function getThemeLabel() {
  switch (theme.value) {
    case 'light': return '浅色'
    case 'dark': return '深色'
    case 'system': return '系统'
  }
}
</script>

<template>
  <button 
    class="theme-toggle"
    @click="handleClick"
    :title="mode === 'cycle' ? `当前: ${getThemeLabel()}` : (isDark ? '切换到浅色模式' : '切换到深色模式')"
    :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
  >
    <span class="icon-wrapper">
      <!-- Sun Icon -->
      <svg 
        v-if="!isDark" 
        class="icon sun" 
        viewBox="0 0 24 24" 
        fill="none" 
        stroke="currentColor" 
        stroke-width="2"
      >
        <circle cx="12" cy="12" r="5"/>
        <line x1="12" y1="1" x2="12" y2="3"/>
        <line x1="12" y1="21" x2="12" y2="23"/>
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
        <line x1="1" y1="12" x2="3" y2="12"/>
        <line x1="21" y1="12" x2="23" y2="12"/>
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
      </svg>
      
      <!-- Moon Icon -->
      <svg 
        v-else 
        class="icon moon" 
        viewBox="0 0 24 24" 
        fill="none" 
        stroke="currentColor" 
        stroke-width="2"
      >
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
      </svg>
    </span>
    
    <span v-if="mode === 'cycle'" class="theme-label">
      {{ getThemeLabel() }}
    </span>
  </button>
</template>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 40px;
  width: auto;
  height: 40px;
  padding: 0 10px;
  background: transparent;
  border: none;
  border-radius: 10px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.icon {
  width: 100%;
  height: 100%;
}

.sun {
  color: #f59e0b;
}

.moon {
  color: #8b5cf6;
}

.theme-label {
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
</style>
