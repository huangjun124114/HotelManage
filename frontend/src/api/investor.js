import request from '@/utils/request'

export const getInvestorList = (params) => request({ url: '/investors', method: 'get', params })
export const createInvestor = (data) => request({ url: '/investors', method: 'post', data })
export const updateInvestor = (id, data) => request({ url: `/investors/${id}`, method: 'put', data })
export const getRelationList = (investorId) => request({ url: '/investor-relations', method: 'get', params: { investorId } })
export const createRelation = (data) => request({ url: '/investor-relations', method: 'post', data })
export const updateRelation = (id, data) => request({ url: `/investor-relations/${id}`, method: 'put', data })
export const deleteRelation = (id) => request({ url: `/investor-relations/${id}`, method: 'delete' })
