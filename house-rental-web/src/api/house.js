import request from './request'

export function getHouseListApi() {
  return request({
    url: '/house/list',
    method: 'get',
  })
}

export function getMyHouseListApi(publisherId) {
  return request({
    url: `/landlord/house/my/${publisherId}`,
    method: 'get',
  })
}

export function getHouseDetailApi(id) {
  return request({
    url: `/house/detail/${id}`,
    method: 'get',
  })
}

export function getLandlordHouseDetailApi(id) {
  return request({
    url: `/landlord/house/detail/${id}`,
    method: 'get',
  })
}

export function getAdminHouseDetailApi(id) {
  return request({
    url: `/admin/house/detail/${id}`,
    method: 'get',
  })
}

export function addHouseApi(data) {
  return request({
    url: '/landlord/house/add',
    method: 'post',
    data,
  })
}

export function updateHouseApi(id, data) {
  return request({
    url: `/landlord/house/update/${id}`,
    method: 'put',
    data,
  })
}

export function updateHouseStatusApi(id, status) {
  return request({
    url: `/landlord/house/status/${id}`,
    method: 'post',
    data: { status },
  })
}

export function deleteHouseApi(id) {
  return request({
    url: `/landlord/house/delete/${id}`,
    method: 'delete',
  })
}

export function getAuditHouseListApi() {
  return request({
    url: '/admin/house/audit/list',
    method: 'get',
  })
}

export function approveAuditHouseApi(id) {
  return request({
    url: `/admin/house/audit/pass/${id}`,
    method: 'post',
  })
}

export function rejectAuditHouseApi(id) {
  return request({
    url: `/admin/house/audit/reject/${id}`,
    method: 'post',
  })
}
