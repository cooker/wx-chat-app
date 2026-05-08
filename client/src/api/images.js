import axios from 'axios'

const http = axios.create({
  baseURL: 'http://localhost:8080/api'
})

export function fetchAlbums() {
  return http.get('/albums')
}

export function fetchAlbumDetail(id) {
  return http.get(`/albums/${id}`)
}

export function reportAlbumView(albumId, visitorId) {
  return http.post('/analytics/track', { albumId, visitorId })
}

export function toAssetUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `http://localhost:8080${path}`
}
