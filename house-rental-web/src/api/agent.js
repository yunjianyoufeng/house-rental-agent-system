import request from './request'

export function agentChatApi(data) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data,
  })
}

export function getAgentHistoryApi() {
  return request({
    url: '/tenant/agent-history',
    method: 'get',
  })
}

export function getAgentHistoryDetailApi(conversationId) {
  return request({
    url: `/tenant/agent-history/${encodeURIComponent(conversationId)}`,
    method: 'get',
  })
}

export async function agentChatStreamApi(data, handlers = {}) {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  const headers = {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json',
  }
  if (userInfo?.token) {
    headers.Authorization = `Bearer ${userInfo.token}`
  }

  const response = await fetch('/api/agent/chat/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify(data),
  })
  if (!response.ok || !response.body) {
    if (response.status === 401) {
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }
    throw new Error('智能租房助手流式请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let donePayload = null

  const handleBlock = (block) => {
    const lines = block.replace(/\r/g, '').split('\n')
    const event = lines.find((line) => line.startsWith('event:'))?.slice(6).trim()
    const dataText = lines
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n')
    if (!event || !dataText) return

    const payload = JSON.parse(dataText)
    if (event === 'delta') handlers.onDelta?.(payload.content || '')
    if (event === 'status') handlers.onStatus?.(payload)
    if (event === 'done') {
      donePayload = payload
      handlers.onDone?.(payload)
    }
    if (event === 'error') {
      throw new Error(payload.message || '智能租房助手服务异常')
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      handleBlock(buffer.slice(0, boundary))
      buffer = buffer.slice(boundary + 2)
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }
  if (buffer.trim()) handleBlock(buffer)
  if (!donePayload) throw new Error('智能租房助手流式响应未完整结束')
  return donePayload
}
