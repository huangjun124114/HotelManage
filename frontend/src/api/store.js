import request from '@/utils/request'

export function getList(params) {
  return request({ url: '/stores', method: 'get', params })
}

export function getById(id) {
  return request({ url: `/stores/${id}`, method: 'get' })
}

export function create(data) {
  return request({ url: '/stores', method: 'post', data })
}

export function update(data) {
  return request({ url: `/stores/${data.id}`, method: 'put', data })
}

export function remove(id) {
  return request({ url: `/stores/${id}`, method: 'delete' })
}

export function getStoreOptions() {
  return request({ url: '/stores/options', method: 'get' })
}
