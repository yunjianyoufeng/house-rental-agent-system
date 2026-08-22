import request from './request'

export function recommendHouseApi(data) {
  return request({
    url: '/recommend/house',
    method: 'post',
    data,
  })
}

export function evaluateRecommendApi(data) {
  return request({
    url: '/recommend/evaluate',
    method: 'post',
    data,
  })
}
