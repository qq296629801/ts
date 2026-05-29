import axios from 'axios'
import { Message } from 'element-ui'
import router from '../router'

const http = axios.create({ timeout: 60000 })

http.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body.code !== 200) {
      Message.error(body.message || '请求失败')
      if (body.code === 401) {
        localStorage.removeItem('accessToken')
        router.push('/login')
      }
      return Promise.reject(body)
    }
    return body.data
  },
  err => {
    Message.error(err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
