import request from './request'

export function notificationUnread() {
  return request.get('/notification/unread')
}

export function notificationUnreadCount() {
  return request.get('/notification/unread/count')
}

export function notificationRead(id) {
  return request.put('/notification/read/' + id)
}

export function notificationReadAll() {
  return request.put('/notification/read/all')
}
