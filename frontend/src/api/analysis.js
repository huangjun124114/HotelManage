import request from '@/utils/request'

export function getDashboard(params) {
  return request({ url: '/reports/dashboard', method: 'get', params })
}

export function getMonthly(params) {
  return request({ url: '/reports/monthly', method: 'get', params })
}

export function getTrend(params) {
  return request({ url: '/reports/trend', method: 'get', params })
}

export function getChannelAnalysis(params) {
  return request({ url: '/reports/channel-analysis', method: 'get', params })
}

export function getStoreRanking(params) {
  return request({ url: '/reports/store-ranking', method: 'get', params })
}
