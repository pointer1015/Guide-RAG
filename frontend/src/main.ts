import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './style.css'
import App from './App.vue'
import { useUserStore } from './stores/user'

const app = createApp(App)

app.use(createPinia())

// 初始化用户状态（从 localStorage 恢复）
const userStore = useUserStore()
userStore.initFromStorage()

app.use(router)

app.mount('#app')
