import axios from 'axios'
import http, { apiBase } from './http.js'

const raw = axios.create({
  baseURL: apiBase
})

raw.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('adminToken')
  if (token) {
    config.headers['X-Admin-Token'] = token
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function adminLogin(password) {
  return raw.post('/auth/admin-login', { password })
}

export function adminLogout() {
  return http.post('/auth/admin-logout')
}
