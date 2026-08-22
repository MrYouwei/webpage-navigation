import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && result.code >= 400) {
      const message = result.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return result
  },
  (error) => {
    if (error.code === 'ECONNABORTED' || error.code === 'ERR_BAD_RESPONSE') {
      ElMessage.error('请求超时，请检查网络或稍后重试')
      return Promise.reject(new Error('请求超时'))
    }
    if (error.response?.status === 401) {
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    const message = error.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

export const api = {
  login: (data) => request.post('/api/auth/login', data),
  register: (data) => request.post('/api/auth/register', data),
  logout: () => request.post('/api/auth/logout'),
  getCurrentUser: () => request.get('/api/auth/me'),

  getNavData: () => request.get('/api/nav/data'),
  saveNavData: (navData) => request.post('/api/nav/data', {
    navData,
    clientVersion: '1.0'
  }),
  updateNavData: (navData) => request.put('/api/nav/data', {
    navData,
    clientVersion: '1.0'
  })
}

export default request
