import request from '../utils/request'

export function getPersonList(params) {
  return request.get('/person/list', { params })
}
export function getPersonDetail(id) {
  return request.get('/person/detail/' + id)
}
export function getEventList(params) {
  return request.get('/event/list', { params })
}
export function getEventDetail(id) {
  return request.get('/event/detail/' + id)
}
export function getLiteratureList(params) {
  return request.get('/literature/list', { params })
}
export function getLiteratureDetail(id) {
  return request.get('/literature/detail/' + id)
}
export function getAllusionList(params) {
  return request.get('/allusion/list', { params })
}
export function getAllusionDetail(id) {
  return request.get('/allusion/detail/' + id)
}
