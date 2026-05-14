import request from '@/utils/request'

export function getTree() {
  return request({ url: '/menus/tree', method: 'get' })
}

export function getUserMenus() {
  return request({ url: '/menus/user', method: 'get' })
}
