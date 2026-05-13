import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 统一的请求工具，所有前端向后端发请求都用这个
// 主要功能：
// 1. 基础路径固定为 /api，这样就不用每次都写完整路径了
// 2. 自动在请求头里带 JWT token，这样后端就知道是谁在请求
// 3. 统一处理后端返回的数据格式：
//    - 成功（code=200）：直接返回数据部分
//    - 未授权（code=401）：清除登录状态，跳转到登录页，还会记住当前页面，登录后自动跳回来
// 开发环境用 sessionStorage，生产环境用 localStorage 存 token
const storage = import.meta.env.DEV ? sessionStorage : localStorage

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  config => {
    const token = storage.getItem('token')
    if (token) config.headers.Authorization = 'Bearer ' + token
    return config
  },
  err => Promise.reject(err)
)

request.interceptors.response.use(
  res => {
    const d = res.data
    if (d.code === 200) return d.data
    if (d.code === 401) {
      const hasToken = !!storage.getItem('token')
      storage.removeItem('token')
      storage.removeItem('user')
      if (hasToken) {
        const cur = router.currentRoute?.value?.fullPath || ''
        const needRedirect = cur && !cur.startsWith('/login') && !cur.startsWith('/register') && !cur.startsWith('/intro')
        router.push(needRedirect ? { path: '/login', query: { redirect: cur } } : '/login')
        ElMessage.error(d.msg || '请重新登录')
      } else {
        ElMessage.warning(d.msg || '请先登录')
      }
      return Promise.reject(new Error(d.msg))
    }
    ElMessage.error(d.msg || '请求失败')
    return Promise.reject(new Error(d.msg))
  },
  err => {
    const msg = err.response?.data?.msg || err.message || '网络错误'
    ElMessage.error(msg)
    if (err.response?.status === 401) {
      const hasToken = !!storage.getItem('token')
      storage.removeItem('token')
      storage.removeItem('user')
      if (hasToken) {
        const cur = router.currentRoute?.value?.fullPath || ''
        const needRedirect = cur && !cur.startsWith('/login') && !cur.startsWith('/register') && !cur.startsWith('/intro')
        router.push(needRedirect ? { path: '/login', query: { redirect: cur } } : '/login')
      }
    }
    return Promise.reject(err)
  }
)

export default request
