import { ref, watch, onMounted } from 'vue'

export type Theme = 'light' | 'dark' | 'system'

const THEME_KEY = 'rag-theme'

// 全局响应式状态
const theme = ref<Theme>('system')
const isDark = ref(true)

// 获取系统主题
function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'dark'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// 应用主题到 DOM
function applyTheme(dark: boolean) {
  const root = document.documentElement
  if (dark) {
    root.classList.add('dark')
    root.classList.remove('light')
  } else {
    root.classList.add('light')
    root.classList.remove('dark')
  }
  isDark.value = dark
}

// 初始化主题
function initTheme() {
  const saved = localStorage.getItem(THEME_KEY) as Theme | null
  if (saved) {
    theme.value = saved
  }
  
  const actualDark = theme.value === 'system' 
    ? getSystemTheme() === 'dark'
    : theme.value === 'dark'
  
  applyTheme(actualDark)
  
  // 监听系统主题变化
  if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (theme.value === 'system') {
        applyTheme(e.matches)
      }
    })
  }
}

export function useTheme() {
  onMounted(() => {
    initTheme()
  })

  watch(theme, (newTheme) => {
    localStorage.setItem(THEME_KEY, newTheme)
    const actualDark = newTheme === 'system' 
      ? getSystemTheme() === 'dark'
      : newTheme === 'dark'
    applyTheme(actualDark)
  })

  function setTheme(newTheme: Theme) {
    theme.value = newTheme
  }

  function toggleTheme() {
    if (theme.value === 'dark') {
      theme.value = 'light'
    } else {
      theme.value = 'dark'
    }
  }

  function cycleTheme() {
    const themes: Theme[] = ['light', 'dark', 'system']
    const currentIndex = themes.indexOf(theme.value)
    theme.value = themes[(currentIndex + 1) % themes.length]
  }

  return {
    theme,
    isDark,
    setTheme,
    toggleTheme,
    cycleTheme
  }
}

// 立即初始化（避免闪烁）
if (typeof window !== 'undefined') {
  initTheme()
}
