import request from './request'

export function getTenantContractListApi(tenantId) {
  return request({
    url: `/tenant/contract/list/${tenantId}`,
    method: 'get',
  })
}

export function getLandlordContractListApi(landlordId) {
  return request({
    url: `/landlord/contract/list/${landlordId}`,
    method: 'get',
  })
}

export function getAdminContractListApi() {
  return request({
    url: '/admin/contract/list',
    method: 'get',
  })
}

export function getContractDetailApi(id) {
  return request({
    url: `/contract/detail/${id}`,
    method: 'get',
  })
}

export function downloadContractFileApi(id) {
  return request({
    url: `/contract/file/${id}`,
    method: 'get',
    responseType: 'blob',
  })
}

export function finishContractLandlordApi(id) {
  return request({
    url: `/landlord/contract/finish/${id}`,
    method: 'post',
  })
}

export function finishContractAdminApi(id) {
  return request({
    url: `/admin/contract/finish/${id}`,
    method: 'post',
  })
}


export function updateLandlordContractFileApi(id, contractUrl) {
  return request({
    url: `/landlord/contract/file/${id}`,
    method: 'put',
    data: { contractUrl },
  })
}

export function updateAdminContractFileApi(id, contractUrl) {
  return request({
    url: `/admin/contract/file/${id}`,
    method: 'put',
    data: { contractUrl },
  })
}
