import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { UserProfile, UserSettings, ThemeMode } from '@/types'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(localStorage.getItem('token'))
  const profile = ref<UserProfile | null>(null)
  const settings = ref<UserSettings>({
    theme: 'dark',
    language: 'zh-CN',
    sendOnEnter: true,
    showSources: true
  })

  // Getters
  // 增加对 profile 的辅助判断，确保 token 和用户信息同步存在时才视为登录状态
  const isLoggedIn = computed(() => Boolean(token.value) && profile.value !== null)
  const userName = computed(() => profile.value?.nickname || profile.value?.username || '用户')
  const userAvatar = computed(() => profile.value?.avatar || '')

  // Actions
  function setAuth(newToken: string, newProfile: UserProfile) {
    token.value = newToken
    profile.value = newProfile
    localStorage.setItem('token', newToken)
    localStorage.setItem('userProfile', JSON.stringify(newProfile))
  }

  function clearAuth() {
    token.value = null
    profile.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userProfile')
    localStorage.removeItem('refreshToken')
  }

  function updateProfile(newProfile: Partial<UserProfile>) {
    if (profile.value) {
      profile.value = { ...profile.value, ...newProfile }
      // 同步写入 localStorage，防止刷新后状态丢失
      localStorage.setItem('userProfile', JSON.stringify(profile.value))
    }
  }

  function setTheme(theme: ThemeMode) {
    settings.value.theme = theme
    // 应用主题到 document
    if (theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  function updateSettings(newSettings: Partial<UserSettings>) {
    settings.value = { ...settings.value, ...newSettings }
  }

  // 初始化时检查本地存储的 profile
  // 初始化时同步后端 profile
  async function fetchProfile() {
    // 只有在 token 存在时才进行网络请求同步
    if (!token.value) return 

    try {
      const { authApi } = await import('@/api/modules')
      const res = await authApi.getMe()
      if (res.data) {
        profile.value = {
          ...profile.value,
          id: String(res.data.id),
          username: res.data.email,
          nickname: res.data.displayName,
          avatar: res.data.avatar,
          createdAt: res.data.gmtCreate
        } as UserProfile
        localStorage.setItem('userProfile', JSON.stringify(profile.value))
      }
    } catch (e) {
      console.error('Fetch profile failed', e)
      // 如果网络请求 401，建议在这里触发 clearAuth，但目前拦截器通常会处理
    }
  }

  function initFromStorage() {
    const storedToken = localStorage.getItem('token')
    const storedProfile = localStorage.getItem('userProfile')

    // 核心逻辑：如果 token 不存在，强制清理 profile
    if (!storedToken) {
      token.value = null
      profile.value = null
      localStorage.removeItem('userProfile')
      return
    }

    token.value = storedToken
    if (storedProfile) {
      try {
        profile.value = JSON.parse(storedProfile)
      } catch {
        localStorage.removeItem('userProfile')
        profile.value = null
      }
    }
  }

  return {
    // State
    token,
    profile,
    settings,
    // Getters
    isLoggedIn,
    userName,
    userAvatar,
    // Actions
    setAuth,
    clearAuth,
    updateProfile,
    setTheme,
    updateSettings,
    initFromStorage,
    fetchProfile
  }
})
