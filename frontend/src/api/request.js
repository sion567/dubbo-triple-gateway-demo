import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

// 请求拦截：带上 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：401 跳登录，403 提示无权限
request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.error('登录已失效，请重新登录')
      router.push('/login')
    } else if (status === 403) {
      ElMessage.error('无权限：' + msg)
    } else {
      ElMessage.error('请求失败：' + msg)
    }
    return Promise.reject(err)
  },
)

export default request
