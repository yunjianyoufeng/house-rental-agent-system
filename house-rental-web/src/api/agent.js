import request from './request'

export function agentChatApi(data) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data,
  })
}
