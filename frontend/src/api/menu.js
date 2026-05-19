import request from '@/utils/request'

export function getTree() {
  return request({ url: '/menus/tree', method: 'get' })
}

export function getUserMenus() {
  return request({ url: '/menus/user', method: 'get' })
}

export function create(data) {
  return request({ url: '/menus', method: 'post', data })
}

export function update(data) {
  return request({ url: `/menus/${data.id}`, method: 'put', data })
}

export function remove(id) {
  return request({ url: `/menus/${id}`, method: 'delete' })
}
