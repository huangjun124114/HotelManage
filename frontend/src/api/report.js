import request from '@/utils/request'

export function getToday(params) {
  return request({ url: '/daily-reports/today', method: 'get', params })
}

export function getDetail(params) {
  return request({ url: '/daily-reports/detail', method: 'get', params })
}

export function saveDraft(data) {
  return request({ url: '/daily-reports/draft', method: 'post', data })
}

export function submit(data) {
  return request({ url: '/daily-reports/submit', method: 'post', data })
}

export function lock(id) {
  return request({ url: `/daily-reports/${id}/lock`, method: 'post' })
}

export function unlock(id) {
  return request({ url: `/daily-reports/${id}/unlock`, method: 'post' })
}

export function reject(id, reason) {
  return request({ url: `/daily-reports/${id}/reject`, method: 'post', data: { reason } })
}

export function queryList(params) {
  return request({ url: '/daily-reports/query', method: 'get', params })
}

export function getTemplateFields(templateId) {
  return request({ url: `/daily-reports/template/${templateId}/fields`, method: 'get' })
}

export function getUnfilledList(params) {
  return request({ url: '/daily-reports/unfilled', method: 'get', params })
}
