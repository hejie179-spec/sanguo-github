import request from '../utils/request'

export function searchGlobal(keyword, limit = 20) {
  return request.get('/search/global', { params: { keyword, limit } })
}

export function searchCategory(keyword, limit = 10) {
  return request.get('/search', { params: { keyword, limit } })
}

export function searchRecommend(limit = 10) {
  return request.get('/search/recommend', { params: { limit } })
}
