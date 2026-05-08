import http from '../../../api/http.js'

export function fetchImages(page = 1, pageSize = 20) {
  return http.get('/images', { params: { page, pageSize } })
}

// 占位：后端管理接口后续接入
export function updateImage(id, payload) {
  return http.put(`/images/${id}`, payload)
}

// 占位：后端管理接口后续接入
export function deleteImage(id) {
  return http.delete(`/images/${id}`)
}

// 占位：后端管理接口后续接入
export function toggleImageStatus(id, online) {
  return http.patch(`/images/${id}/status`, { online })
}
