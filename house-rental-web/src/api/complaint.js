import request from './request'

export function addComplaintApi(data) {
  return request({
    url: '/tenant/complaint/add',
    method: 'post',
    data,
  })
}

export function getTenantComplaintListApi(userId) {
  return request({
    url: `/tenant/complaint/list/${userId}`,
    method: 'get',
  })
}

export function getAdminComplaintListApi() {
  return request({
    url: '/admin/complaint/list',
    method: 'get',
  })
}

export function processComplaintApi(id, data) {
  return request({
    url: `/admin/complaint/process/${id}`,
    method: 'post',
    data,
  })
}
