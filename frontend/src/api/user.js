import request from '@/utils/request'

export function getList(params) {
  return request({ url: '/users', method: 'get', params })
}

export function getById(id) {
  return request({ url: `/users/${id}`, method: 'get' })
}

export function create(data) {
  return request({ url: '/users', method: 'post', data })
}

export function update(data) {
  return request({ url: `/users/${data.id}`, method: 'put', data })
}

export function resetPassword(id, data) {
  return request({ url: `/users/${id}/reset-password`, method: 'put', data })
}

export function updateStatus(id, status) {
  return request({ url: `/users/${id}/status`, method: 'put', data: { status } })
}
