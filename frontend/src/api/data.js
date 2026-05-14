import request from '@/utils/request'

export function importExcel(formData) {
  return request({
    url: '/data/import-excel',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
