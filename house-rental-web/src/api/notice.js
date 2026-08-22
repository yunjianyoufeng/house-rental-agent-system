import request from './request'

export function getNoticeListApi() {
  return request({
    url: '/notice/list',
    method: 'get',
  })
}

export function addNoticeApi(data) {
  return request({
    url: '/admin/notice/add',
    method: 'post',
    data,
  })
}

export function updateNoticeApi(id, data) {
  return request({
    url: `/admin/notice/update/${id}`,
    method: 'put',
    data,
  })
}

export function deleteNoticeApi(id) {
  return request({
    url: `/admin/notice/delete/${id}`,
    method: 'delete',
  })
}
