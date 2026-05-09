import axios from 'axios'

export const apiBase = import.meta.env.VITE_API_BASE || '/api'

const http = axios.create({
  baseURL: apiBase
})

function redirectToLogin() {
  sessionStorage.removeItem('adminToken')
  const base = import.meta.env.BASE_URL || '/'
  const basePrefix = base === '/' ? '' : base.replace(/\/$/, '')
  const currentPath = window.location.pathname
  const relativePath =
    basePrefix && currentPath.startsWith(basePrefix) ? currentPath.slice(basePrefix.length) || '/' : currentPath
  if (!relativePath.startsWith('/login')) {
    window.location.replace(`${base}login?redirect=${encodeURIComponent(relativePath + window.location.search)}`)
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
