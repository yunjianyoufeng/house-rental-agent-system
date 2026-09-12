import test from 'node:test'
import assert from 'node:assert/strict'
import { csrfHeaders } from '../src/api/csrf.js'

test('只读取独立 CSRF Cookie，不发送会话 Cookie 内容', async () => {
  globalThis.document = { cookie: 'rental_session=hidden; XSRF-TOKEN=csrf-value' }
  globalThis.fetch = () => { throw new Error('已有令牌时不应发请求') }
  assert.deepEqual(await csrfHeaders(), { 'X-XSRF-TOKEN': 'csrf-value' })
})

test('并发请求共用一次 CSRF 初始化，并携带同源 Cookie', async () => {
  globalThis.document = { cookie: '' }
  let requests = 0
  globalThis.fetch = async (url, options) => {
    requests++
    assert.equal(url, '/api/auth/csrf')
    assert.equal(options.credentials, 'same-origin')
    await Promise.resolve()
    document.cookie = 'XSRF-TOKEN=new-csrf-value'
    return { ok: true }
  }
  const [first, second] = await Promise.all([csrfHeaders(), csrfHeaders()])
  assert.equal(requests, 1)
  assert.deepEqual(first, second)
  assert.equal(first['X-XSRF-TOKEN'], 'new-csrf-value')
})

test('初始化失败时拒绝继续发送业务请求', async () => {
  globalThis.document = { cookie: '' }
  globalThis.fetch = async () => ({ ok: false })
  await assert.rejects(csrfHeaders(), /安全校验初始化失败/)
  globalThis.fetch = async () => ({ ok: true })
  await assert.rejects(csrfHeaders(), /安全校验不可用/)
})
