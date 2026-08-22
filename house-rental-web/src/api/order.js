import request from './request'

export function getTenantOrderListApi(tenantId) {
  return request({
    url: `/tenant/order/list/${tenantId}`,
    method: 'get',
  })
}

export function getLandlordOrderListApi(landlordId) {
  return request({
    url: `/landlord/order/list/${landlordId}`,
    method: 'get',
  })
}

export function getAdminOrderListApi() {
  return request({
    url: '/admin/order/list',
    method: 'get',
  })
}

export function startOrderPaymentApi(id, data) {
  return request({
    url: `/tenant/order/pay/start/${id}`,
    method: 'post',
    data,
  })
}

export function payOrderApi(id, data) {
  return request({
    url: `/tenant/order/pay/${id}`,
    method: 'post',
    data,
  })
}

export function cancelOrderApi(id) {
  return request({
    url: `/tenant/order/cancel/${id}`,
    method: 'post',
  })
}
