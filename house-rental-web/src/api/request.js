import axios from 'axios'
import { ElMessage } from 'element-plus'
import { csrfHeaders } from './csrf'

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
  async (config) => {
    if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) {
      Object.assign(config.headers, await csrfHeaders())
    }

    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      const contentType = response.headers['content-type'] || response.data?.type || ''
      if (contentType.includes('application/json')) {
        return response.data.text().then((text) => {
          let res
          try {
            res = JSON.parse(text)
          } catch {
            res = { message: '合同附件响应解析失败' }
          }
          if (res.code === 401) {
            clearLoginAndRedirect()
          }
          ElMessage.error(res.message || '合同附件下载失败')
          return Promise.reject(res)
        })
      }
      const filename = response.headers['content-disposition']?.match(/filename="([a-fA-F0-9]+\.[a-z]+)"/)?.[1]
      return filename ? new File([response.data], filename, { type: response.data.type }) : response.data
    }

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
