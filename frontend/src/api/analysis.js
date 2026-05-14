import request from '@/utils/request'

export function getDashboard(params) {
  return request({ url: '/analysis/dashboard', method: 'get', params })
}

export function getMonthly(params) {
  return request({ url: '/analysis/monthly', method: 'get', params })
}

export function getTrend(params) {
  return request({ url: '/analysis/trend', method: 'get', params })
}

export function getChannelAnalysis(params) {
  return request({ url: '/analysis/channel', method: 'get', params })
}

export function getStoreRanking(params) {
  return request({ url: '/analysis/ranking', method: 'get', params })
}
