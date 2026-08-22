import request from './request'

export function addAppointmentApi(data) {
  return request({
    url: '/tenant/appointment/add',
    method: 'post',
    data,
  })
}

export function getTenantAppointmentListApi(tenantId) {
  return request({
    url: `/tenant/appointment/list/${tenantId}`,
    method: 'get',
  })
}

export function getLandlordAppointmentListApi(landlordId) {
  return request({
    url: `/landlord/appointment/list/${landlordId}`,
    method: 'get',
  })
}

export function approveAppointmentApi(id) {
  return request({
    url: `/landlord/appointment/approve/${id}`,
    method: 'post',
  })
}

export function rejectAppointmentApi(id) {
  return request({
    url: `/landlord/appointment/reject/${id}`,
    method: 'post',
  })
}
