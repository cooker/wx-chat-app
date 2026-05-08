import http from '../../../api/http.js'

export function uploadAlbumImage(file, folder) {
  const formData = new FormData()
  formData.append('file', file)
  if (folder) {
    formData.append('folder', folder)
  }
  return http.post('/files/images', formData)
}

export function fetchAlbums(page = 1, pageSize = 15) {
  return http.get('/albums', { params: { page, pageSize } })
}

export function fetchAlbumDetail(id) {
  return http.get(`/albums/${id}`)
}

export function createAlbum(payload) {
  return http.post('/albums', payload)
}

export function updateAlbum(id, payload) {
  return http.put(`/albums/${id}`, payload)
}

export function deleteAlbum(id) {
  return http.delete(`/albums/${id}`)
}
