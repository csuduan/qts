import request from '@/utils/request'

export function getList(params) {
  return request({
    url: '/v1/table/list',
    method: 'get',
    params
  })
}
