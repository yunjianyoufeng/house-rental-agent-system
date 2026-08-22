import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

function clearLoginAndRedirect() {
  localStorage.removeItem('userInfo')
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

request.interceptors.request.use(
  (config) => {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')

    if (userInfo?.token) {
      config.headers.Authorization = `Bearer ${userInfo.token}`
    }

    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code === 200) {
      return res
    }

    if (res.code === 401) {
      ElMessage.error(res.message || '登录已过期，请重新登录')
      clearLoginAndRedirect()
      return Promise.reject(res)
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
    const res = error.response?.data

    if (error.response?.status === 401 || res?.code === 401) {
      ElMessage.error(res?.message || '登录已过期，请重新登录')
      clearLoginAndRedirect()
      return Promise.reject(res || error)
    }

    ElMessage.error(res?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

export default request
