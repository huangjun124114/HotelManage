import request from '@/utils/request'

export function getList(params) {
  return request({ url: '/roles', method: 'get', params })
}

export function getById(id) {
  return request({ url: `/roles/${id}`, method: 'get' })
}

export function create(data) {
  return request({ url: '/roles', method: 'post', data })
}

export function update(data) {
  return request({ url: `/roles/${data.id}`, method: 'put', data })
}

export function getRoleMenus(id) {
  return request({ url: `/roles/${id}/menus`, method: 'get' })
}

export function saveRoleMenus(id, menuIds) {
  return request({ url: `/roles/${id}/menus`, method: 'put', data: { menuIds } })
}
