import request from './request'

export function addRentalApplicationApi(data) {
  return request({
    url: '/tenant/application/add',
    method: 'post',
    data,
  })
}

export function getTenantApplicationListApi(tenantId) {
  return request({
    url: `/tenant/application/list/${tenantId}`,
    method: 'get',
  })
}

export function getLandlordApplicationListApi(landlordId) {
  return request({
    url: `/landlord/application/list/${landlordId}`,
    method: 'get',
  })
}

export function getAdminApplicationListApi() {
  return request({
    url: '/admin/application/list',
    method: 'get',
  })
}

export function approveRentalApplicationApi(id) {
  return request({
    url: `/landlord/application/approve/${id}`,
    method: 'post',
  })
}

export function rejectRentalApplicationApi(id) {
  return request({
    url: `/landlord/application/reject/${id}`,
    method: 'post',
  })
}
