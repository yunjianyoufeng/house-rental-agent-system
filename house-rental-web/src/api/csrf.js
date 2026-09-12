let pendingRequest = null

function readCsrfCookie() {
  const cookie = document.cookie.split('; ').find((item) => item.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null
}

export async function csrfHeaders() {
  // 会话 Cookie 为 HttpOnly；JS 只读取独立的 CSRF 令牌，不接触登录凭据。
  if (!readCsrfCookie()) {
    if (!pendingRequest) {
      pendingRequest = fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' })
        .then((response) => {
          if (!response.ok) throw new Error('安全校验初始化失败，请刷新页面重试')
        })
        .finally(() => { pendingRequest = null })
    }
    await pendingRequest
  }
  const token = readCsrfCookie()
  if (!token) throw new Error('安全校验不可用，请检查浏览器 Cookie 设置')
  return { 'X-XSRF-TOKEN': token }
}
