import request from './request'

export function getStatisticsOverviewApi() {
  return request({
    url: '/admin/statistics/overview',
    method: 'get',
  })
}
