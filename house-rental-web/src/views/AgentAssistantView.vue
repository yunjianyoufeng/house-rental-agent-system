<template>
  <DashboardShell
    title="智能租房助手"
    :role="userInfo?.roleCode || ''"
    :menus="menus"
    active-menu="agent"
    @menu-click="handleMenuClick"
    @logout="logout"
  >
    <el-card class="assistant-card">
      <template #header>
        <div class="assistant-header">
          <div>
            <div class="assistant-title">AI 租房决策助手</div>
            <div class="assistant-subtitle">可以查询真实房源、查看详情并说明推荐理由</div>
          </div>
          <el-tag type="success" effect="light">Agent + Tools</el-tag>
        </div>
      </template>

      <div ref="messageListRef" class="message-list">
        <div
          v-for="(item, index) in messages"
          :key="index"
          class="message-row"
          :class="`message-row-${item.role}`"
        >
          <div class="message-role">{{ item.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-content">
            <div class="message-bubble">
              <span v-if="item.content">{{ item.content }}</span>
              <span v-else-if="item.streaming" class="message-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                {{ item.status || '正在分析需求…' }}
              </span>
              <span v-if="item.streaming && item.content" class="streaming-cursor"></span>
            </div>
            <div
              v-if="item.role === 'assistant' && item.sources?.length"
              class="knowledge-sources"
            >
              <div class="knowledge-sources-title">知识来源</div>
              <div
                v-for="source in item.sources"
                :key="`${source.source}-${source.section}`"
                class="knowledge-source-item"
              >
                <el-tag size="small" type="success" effect="plain">RAG</el-tag>
                <div>
                  <div class="knowledge-source-name">{{ source.title }}</div>
                  <div class="knowledge-source-meta">
                    {{ source.section || source.source }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>

      <div class="assistant-input">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          resize="none"
          placeholder="例如：帮我找聊城1600元以内、安静的一室一厅"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="input-actions">
          <span class="input-tip">Enter 发送，Shift + Enter 换行</span>
          <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
        </div>
      </div>
    </el-card>
  </DashboardShell>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import DashboardShell from '../components/DashboardShell.vue'
import { agentChatStreamApi } from '../api/agent'
import { logoutApi } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const menus = [
  { key: 'agent', label: '智能租房助手' },
  { key: 'recommend', label: '智能找房' },
  { key: 'appointments', label: '我的预约' },
  { key: 'applications', label: '我的申请' },
  { key: 'contracts', label: '我的合同' },
  { key: 'orders', label: '我的订单' },
  { key: 'repairs', label: '我的报修' },
  { key: 'complaints', label: '我的投诉' },
]

const inputMessage = ref('')
const sending = ref(false)
const messageListRef = ref(null)
const defaultMessages = [
  {
    role: 'assistant',
    content: '你好，我可以根据预算、城市、区域、户型和居住偏好查询平台中的真实房源。请告诉我你的租房需求。',
  },
]
const conversationStorageKey = `agentConversation:${userInfo.value?.id || 'tenant'}`

const loadStoredConversation = () => {
  try {
    const stored = JSON.parse(sessionStorage.getItem(conversationStorageKey) || 'null')
    const isActive = stored?.updatedAt > Date.now() - 60 * 60 * 1000
    if (isActive && stored?.conversationId && Array.isArray(stored?.messages)) {
      return stored
    }
  } catch (error) {
    console.warn('会话恢复失败，将创建新会话', error)
  }
  return null
}

const storedConversation = loadStoredConversation()
const conversationId =
  storedConversation?.conversationId ||
  globalThis.crypto?.randomUUID?.() ||
  `conversation-${Date.now()}`
const messages = ref(storedConversation?.messages || defaultMessages)

const persistConversation = () => {
  sessionStorage.setItem(
    conversationStorageKey,
    JSON.stringify({
      conversationId,
      messages: messages.value.slice(-12),
      updatedAt: Date.now(),
    }),
  )
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const normalizeAssistantText = (content) => {
  return String(content || '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/^\s*---\s*$/gm, '')
    .trim()
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message) {
    ElMessage.warning('请输入租房需求')
    return
  }
  if (sending.value) return

  messages.value.push({ role: 'user', content: message })
  messages.value.push({
    role: 'assistant',
    content: '',
    sources: [],
    streaming: true,
    status: '正在分析需求…',
  })
  const assistantMessage = messages.value[messages.value.length - 1]
  persistConversation()
  inputMessage.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    await agentChatStreamApi(
      { message, conversationId },
      {
        onDelta: (content) => {
          assistantMessage.content += content
          assistantMessage.status = ''
          scrollToBottom()
        },
        onStatus: () => {
          assistantMessage.status = '正在调用平台工具…'
        },
        onDone: (payload) => {
          assistantMessage.content = normalizeAssistantText(
            payload.answer || assistantMessage.content,
          )
          assistantMessage.sources = Array.isArray(payload.sources) ? payload.sources : []
        },
      },
    )
    persistConversation()
  } catch (error) {
    if (!assistantMessage.content) {
      assistantMessage.content = error.message || '助手暂未返回有效内容，请稍后重试。'
    }
    ElMessage.error(error.message || '智能租房助手服务异常')
  } finally {
    assistantMessage.streaming = false
    assistantMessage.status = ''
    sending.value = false
    await scrollToBottom()
  }
}

const handleMenuClick = (key) => {
  if (key === 'agent') return
  router.push({ name: 'tenant', query: { menu: key } })
}

const logout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    console.error(error)
  } finally {
    sessionStorage.removeItem(conversationStorageKey)
    userStore.clearUserInfo()
    router.push('/login')
  }
}
</script>

<style scoped>
.assistant-card {
  max-width: 1080px;
  margin: 0 auto;
  border-radius: 20px;
}

.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.assistant-title {
  font-size: 20px;
  font-weight: 700;
}

.assistant-subtitle {
  margin-top: 6px;
  color: #606266;
  font-size: 14px;
}

.message-list {
  height: min(58vh, 600px);
  min-height: 380px;
  overflow-y: auto;
  padding: 8px 10px 20px;
  scroll-behavior: smooth;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 18px 0;
}

.message-row-user {
  flex-direction: row-reverse;
}

.message-role {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 700;
  color: #fff;
  background: #409eff;
}

.message-row-user .message-role {
  background: #67c23a;
}

.message-bubble {
  padding: 13px 16px;
  border-radius: 6px 18px 18px;
  background: #f2f6fc;
  color: #303133;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-content {
  max-width: min(76%, 760px);
}

.message-row-user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.knowledge-sources {
  margin-top: 10px;
  padding: 12px 14px;
  border: 1px solid #d9ecff;
  border-radius: 12px;
  background: #f5faff;
}

.knowledge-sources-title {
  margin-bottom: 9px;
  color: #606266;
  font-size: 13px;
  font-weight: 700;
}

.knowledge-source-item {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.knowledge-source-item + .knowledge-source-item {
  margin-top: 9px;
}

.knowledge-source-name {
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.knowledge-source-meta {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.message-row-user .message-bubble {
  color: #fff;
  background: #409eff;
  border-radius: 18px 6px 18px 18px;
}

.message-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1.1em;
  margin-left: 3px;
  vertical-align: -2px;
  background: #409eff;
  animation: cursor-blink 0.8s steps(1) infinite;
}

@keyframes cursor-blink {
  50% {
    opacity: 0;
  }
}

.assistant-input {
  padding-top: 18px;
  border-top: 1px solid #ebeef5;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.input-tip {
  color: #909399;
  font-size: 13px;
}

@media (max-width: 767px) {
  .assistant-header,
  .input-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .message-list {
    height: 52vh;
    min-height: 320px;
  }

  .message-content {
    max-width: 82%;
  }

  .input-actions .el-button {
    width: 100%;
  }
}
</style>
