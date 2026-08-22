import request from './request'

export function addRepairApi(data) {
  return request({
    url: '/tenant/repair/add',
    method: 'post',
    data,
  })
}

export function getTenantRepairListApi(tenantId) {
  return request({
    url: `/tenant/repair/list/${tenantId}`,
    method: 'get',
  })
}

export function getLandlordRepairListApi(landlordId) {
  return request({
    url: `/landlord/repair/list/${landlordId}`,
    method: 'get',
  })
}

export function getAdminRepairListApi() {
  return request({
    url: '/admin/repair/list',
    method: 'get',
  })
}

export function processRepairApi(id, data) {
  return request({
    url: `/landlord/repair/process/${id}`,
    method: 'post',
    data,
  })
}
