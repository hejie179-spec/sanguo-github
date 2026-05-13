import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'

export const useNotificationStore = defineStore('notification', () => {
  const storage = import.meta.env.DEV ? sessionStorage : localStorage
  const count = ref(0)
  const listeners = []
  let emitter = null

  function addListener(fn) {
    listeners.push(fn)
  }

  function removeListener(fn) {
    const idx = listeners.indexOf(fn)
    if (idx > -1) listeners.splice(idx, 1)
  }

  function notify(type, content, link) {
    count.value++
    listeners.forEach(fn => fn({ type, content, link }))
  }

  async function refreshCount() {
    try {
      const c = await request.get('/notification/unread/count')
      count.value = c || 0
    } catch {
      count.value = 0
    }
  }

  function connectSse() {
    const token = storage.getItem('token')
    if (!token) return
    if (emitter) return
    const url = '/api/notification/subscribe?token=' + encodeURIComponent(token)
    emitter = new EventSource(url)
    emitter.onmessage = (e) => {
      const parts = (e.data || '').split('|')
      if (parts.length >= 2) notify(parts[0], parts[1], parts[2] || '')
    }
    emitter.onerror = () => {
      emitter.close()
      emitter = null
      setTimeout(connectSse, 8000)
    }
  }

  function disconnectSse() {
    if (emitter) {
      emitter.close()
      emitter = null
    }
  }

  return { count, addListener, removeListener, notify, refreshCount, connectSse, disconnectSse }
})
