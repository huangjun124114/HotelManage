<template>
  <div class="message-bell">
    <el-popover
      placement="bottom-end"
      :width="360"
      trigger="click"
      @show="onPopoverShow"
    >
      <template #reference>
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
          <el-icon class="bell-icon" :class="{ 'has-unread': unreadCount > 0 }">
            <Bell />
          </el-icon>
        </el-badge>
      </template>

      <div class="message-panel">
        <div class="message-header">
          <span class="message-title">站内消息</span>
          <el-button
            v-if="unreadCount > 0"
            type="primary"
            link
            size="small"
            @click="handleMarkAllRead"
          >
            全部已读
          </el-button>
        </div>

        <div class="message-list" v-loading="loading">
          <template v-if="messages.length > 0">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ unread: msg.isRead === 0 }"
              @click="handleClickMessage(msg)"
            >
              <div class="message-item-header">
                <span class="message-item-title">{{ msg.title || '系统通知' }}</span>
                <span class="message-item-time">{{ formatTime(msg.createTime) }}</span>
              </div>
              <div class="message-item-content">{{ msg.content }}</div>
              <div v-if="msg.isRead === 0" class="unread-dot"></div>
            </div>
          </template>
          <div v-else class="message-empty">
            <el-icon :size="32" color="#c0c4cc"><BellFilled /></el-icon>
            <p>暂无消息</p>
          </div>
        </div>

        <div class="message-footer" v-if="messages.length > 0">
          <el-button type="primary" link @click="goToMessageCenter">查看全部消息</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, BellFilled } from '@element-plus/icons-vue'
import { getUnreadCount, getRecentMessages, markRead, markAllRead } from '@/api/message'

const router = useRouter()
const unreadCount = ref(0)
const messages = ref([])
const loading = ref(false)
let pollTimer = null

async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.unreadCount ?? 0
  } catch (e) { /* ignore */ }
}

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getRecentMessages(10)
    messages.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function handleClickMessage(msg) {
  if (msg.isRead === 0) {
    try {
      await markRead(msg.id)
      msg.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) { /* ignore */ }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    messages.value.forEach(m => m.isRead = 1)
    unreadCount.value = 0
  } catch (e) { /* ignore */ }
}

function onPopoverShow() {
  fetchMessages()
}

function goToMessageCenter() {
  router.push('/messages')
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  return timeStr.substring(0, 10)
}

onMounted(() => {
  fetchUnreadCount()
  // 30秒轮询一次
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.message-bell {
  display: flex;
  align-items: center;
}

.bell-badge {
  line-height: 1;
}

.bell-icon {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.bell-icon:hover {
  color: #409eff;
}

.bell-icon.has-unread {
  color: #409eff;
  animation: bell-shake 0.5s ease-in-out;
}

@keyframes bell-shake {
  0%, 100% { transform: rotate(0); }
  20% { transform: rotate(15deg); }
  40% { transform: rotate(-10deg); }
  60% { transform: rotate(5deg); }
  80% { transform: rotate(-3deg); }
}

.message-panel {
  margin: -12px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.message-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.message-list {
  max-height: 400px;
  overflow-y: auto;
}

.message-item {
  position: relative;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:hover {
  background: #f5f7fa;
}

.message-item.unread {
  background: #ecf5ff;
}

.message-item.unread:hover {
  background: #d9ecff;
}

.message-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.message-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.message-item-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  margin-left: 8px;
}

.message-item-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  position: absolute;
  top: 12px;
  left: 6px;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
}

.message-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #909399;
}

.message-empty p {
  margin-top: 8px;
  font-size: 14px;
}

.message-footer {
  padding: 8px 16px;
  text-align: center;
  border-top: 1px solid #f0f0f0;
}
</style>
