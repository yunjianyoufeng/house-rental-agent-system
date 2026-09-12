import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/global.css'

import App from './App.vue'
import router from './router'

// 清除旧版本持久化的 Bearer 凭据，升级后重新登录获取 HttpOnly 会话。
try {
  const previous = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (previous?.token) localStorage.removeItem('userInfo')
} catch {
  localStorage.removeItem('userInfo')
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
