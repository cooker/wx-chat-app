import axios from 'axios'

const http = axios.create({
  baseURL: 'http://localhost:8080/api'
})

function redirectToLogin() {
  sessionStorage.removeItem('adminToken')
  const path = window.location.pathname
  if (!path.startsWith('/login')) {
    window.location.replace(`/login?redirect=${encodeURIComponent(path + window.location.search)}`)
  }
}

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('adminToken')
  if (token) {
    config.headers['X-Admin-Token'] = token
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    if (res?.data?.code === 401) {
      redirectToLogin()
      return Promise.reject(new Error(res?.data?.message || '未登录或会话已过期'))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      const url = String(err.config?.url ?? '')
      if (!url.includes('admin-login')) {
        redirectToLogin()
      }
    }
    return Promise.reject(err)
  }
)

export default http
