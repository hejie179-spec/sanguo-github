import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'

// 用户登录状态管理，这是前端鉴权的核心，所有关于登录状态的操作都在这里
// 为什么用Pinia？因为它是Vue 3推荐的状态管理工具，比Vuex更简单直观
// defineStore：定义一个状态管理仓库，第一个参数是仓库名，第二个参数是一个函数
// ref：Vue 3的响应式API，用来创建响应式数据
// storage：根据环境选择存储方式，开发环境用sessionStorage（关闭浏览器就没了），生产环境用localStorage（持久化存储）
export const useUserStore = defineStore('user', () => {
  // 初始化存储方式
  const storage = import.meta.env.DEV ? sessionStorage : localStorage
  // 从存储中获取token，如果没有就为空字符串
  const token = ref(storage.getItem('token') || '')
  // 从存储中获取用户信息，如果没有就为null
  const user = ref(null)

  // 设置token的方法，同时保存到存储中
  function setToken(t) {
    token.value = t
    storage.setItem('token', t)
  }

  // 设置用户信息的方法，同时保存到存储中
  // 如果用户信息为空，就从存储中删除
  function setUser(u) {
    user.value = u
    if (u) storage.setItem('user', JSON.stringify(u))
    else storage.removeItem('user')
  }

  // 登出方法，清空token和用户信息
  function logout() {
    token.value = ''
    user.value = null
    storage.removeItem('token')
    storage.removeItem('user')
  }

  // 获取用户信息的方法，异步函数
  async function fetchInfo() {
    // 如果没有token，直接返回
    if (!token.value) return
    try {
      // 调用后端接口获取用户信息，包括角色等
      const data = await request.get('/auth/info')
      // 设置用户信息
      setUser(data)
      return data
    } catch {
      // 如果获取失败，说明token可能过期了，直接登出
      logout()
    }
  }

  // 从存储中初始化用户状态
  function initFromStorage() {
    const t = storage.getItem('token')
    if (t != null) token.value = t
    const u = storage.getItem('user')
    if (u) user.value = JSON.parse(u)
  }

  // 返回仓库的状态和方法，供其他组件使用
  return { token, user, setToken, setUser, logout, fetchInfo, initFromStorage }
})
