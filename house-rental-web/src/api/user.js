import request from './request'

export function getAdminUserListApi() {
  return request({
    url: '/admin/users',
    method: 'get',
  })
}

export function updateUserStatusApi(id, status) {
  return request({
    url: `/admin/users/status/${id}`,
    method: 'post',
    data: { status },
  })
}
