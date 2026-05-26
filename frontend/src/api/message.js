import request from '@/utils/request'

export function getUnreadCount() {
  return request({ url: '/messages/unread-count', method: 'get' })
}

export function getRecentMessages(limit = 10) {
  return request({ url: '/messages/recent', method: 'get', params: { limit } })
}

export function markRead(id) {
  return request({ url: `/messages/${id}/read`, method: 'put' })
}

export function markAllRead() {
  return request({ url: '/messages/read-all', method: 'put' })
}

export function notifyStore(data) {
  return request({ url: '/messages/notify-store', method: 'post', data })
}

export function notifyUnfilled(data) {
  return request({ url: '/messages/notify-unfilled', method: 'post', data })
}
