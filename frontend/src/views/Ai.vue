<template>
  <div class="ai-page">
    <div class="sg-page-title">
      <div>
        <h1>AI 智能助手</h1>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card class="sg-card chat-card">
          <template #header>
            <div class="chat-head">
              <div class="card-head">对话</div>
              <div class="chat-actions">
                <el-button size="small" @click="newConversation">新对话</el-button>
              </div>
            </div>
          </template>
          <div ref="chatBodyRef" class="chat-body">
            <div v-if="messages.length === 0" class="sg-muted empty">
              直接提问即可开始多轮对话；也可以点击右侧历史记录进入并继续提问。
            </div>
            <div v-for="(m, idx) in messages" :key="idx" class="msg" :class="m.role">
              <div class="bubble">{{ m.content }}</div>
              <div v-if="m.time" class="time">{{ m.time }}</div>
            </div>
          </div>

          <div class="sg-muted hint">建议写清人物/事件名，并提出具体问题，例如“诸葛亮的主要功绩是什么”。</div>
          <div class="ask">
            <el-input v-model="question" type="textarea" placeholder="输入你关于三国文化的问题..." :rows="4" />
            <div class="ask-actions">
              <el-button type="primary" :loading="asking" @click="ask">提问</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card class="sg-card">
          <template #header>
            <div class="card-head">历史记录</div>
          </template>
          <el-table :data="history" v-loading="historyLoading" class="table" @row-click="openFromHistory">
            <el-table-column prop="questionContent" label="提问" min-width="180" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import request from '../utils/request'

const question = ref('')
const asking = ref(false)
const conversationId = ref('')
const seedRecordId = ref(null)
const messages = ref([])
const history = ref([])
const historyLoading = ref(false)
const chatBodyRef = ref(null)

function nowText() {
  return new Date().toLocaleString('zh-CN')
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function ask() {
  if (!question.value.trim()) return
  asking.value = true
  try {
    const q = question.value.trim()
    question.value = ''
    messages.value.push({ role: 'user', content: q, time: nowText() })
    scrollToBottom()

    const data = await request.post('/ai/ask', {
      questionContent: q,
      conversationId: conversationId.value || undefined,
      seedRecordId: seedRecordId.value || undefined,
      maxHistoryTurns: 6
    })

    if (data && data.conversationId) {
      conversationId.value = data.conversationId
      seedRecordId.value = null
    }
    messages.value.push({ role: 'assistant', content: data?.answerContent || '', time: data?.createTime ? new Date(data.createTime).toLocaleString('zh-CN') : nowText() })
    scrollToBottom()

    if (data) {
      const row = { ...data, createTime: data.createTime ? new Date(data.createTime).toLocaleString('zh-CN') : '' }
      history.value = [row, ...history.value.filter(r => r.id !== row.id)]
    }
  } finally { asking.value = false }
}

function newConversation() {
  conversationId.value = ''
  seedRecordId.value = null
  messages.value = []
  question.value = ''
}

async function openFromHistory(row) {
  if (!row) return

  const baseMessages = []
  if (row.questionContent) baseMessages.push({ role: 'user', content: row.questionContent, time: row.createTime || '' })
  if (row.answerContent) baseMessages.push({ role: 'assistant', content: row.answerContent, time: row.createTime || '' })

  const cid = row.conversationId || ''
  if (cid && !String(cid).startsWith('legacy-')) {
    conversationId.value = cid
    seedRecordId.value = null
    try {
      const records = await request.get('/ai/conversation/records', { params: { conversationId: cid } })
      const ms = []
      ;(records || []).forEach(r => {
        const t = r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : ''
        if (r.questionContent) ms.push({ role: 'user', content: r.questionContent, time: t })
        if (r.answerContent) ms.push({ role: 'assistant', content: r.answerContent, time: t })
      })
      messages.value = ms.length ? ms : baseMessages
    } catch {
      messages.value = baseMessages
    }
  } else {
    conversationId.value = ''
    seedRecordId.value = row.id
    messages.value = baseMessages
  }
  scrollToBottom()
}

onMounted(async () => {
  historyLoading.value = true
  try {
    const res = await request.get('/ai/history', { params: { current: 1, size: 20 } })
    history.value = (res || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
  } finally { historyLoading.value = false }
})
</script>

<style lang="scss" scoped>
.card-head {
  font-weight: 900;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-body {
  height: 34.5vh; /* 占视口高度的60%，自适应屏幕 */
  overflow: auto;
  padding: 4px 2px 8px;
}

.empty {
  padding: 12px 6px;
}

.msg {
  display: flex;
  flex-direction: column;
  margin: 10px 0;
}

.msg.user {
  align-items: flex-end;
}

.msg.assistant {
  align-items: flex-start;
}

.bubble {
  max-width: 92%;
  padding: 10px 12px;
  border-radius: 10px;
  white-space: pre-wrap;
  line-height: 22px;
  background: #f5f7fa;
}

.msg.user .bubble {
  background: #ecf5ff;
}

.time {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
}

.ask {
  margin-top: 12px;
}

.ask-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.table {
  width: 100%;
}
</style>
