import request from '@/utils/request'

export function fetchList(query) {
  return request({
    url: '/v1/task/list',
    method: 'get',
    params: query
  })
}
