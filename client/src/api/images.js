import axios from 'axios'

const apiBase = import.meta.env.VITE_API_BASE || '/api'

const http = axios.create({
  baseURL: apiBase
})

let imageCdnBase = ''
let uploadServerOrigin = ''
let feedPageSize = 8

function stripTrailingSlashes(s) {
  let t = s
  while (t.endsWith('/')) {
    t = t.slice(0, -1)
  }
  return t
}

export function setImageCdnBase(value) {
  if (typeof value !== 'string' || !value.trim()) {
    imageCdnBase = ''
    return
  }
  imageCdnBase = stripTrailingSlashes(value.trim())
}

export function setUploadServerOrigin(value) {
  if (typeof value !== 'string' || !value.trim()) {
    uploadServerOrigin = ''
    return
  }
  uploadServerOrigin = stripTrailingSlashes(value.trim())
}

function inferOriginFromViteApiBase() {
  const base = import.meta.env.VITE_API_BASE || '/api'
  if (typeof base !== 'string' || !/^https?:\/\//i.test(base)) {
    return ''
  }
  try {
    return stripTrailingSlashes(new URL(base).origin)
  } catch {
    return ''
  }
}

function effectiveUploadOrigin() {
  return stripTrailingSlashes(uploadServerOrigin) || inferOriginFromViteApiBase()
}

export function setFeedPageSize(value) {
  const n = typeof value === 'number' ? value : parseInt(String(value ?? '').trim(), 10)
  if (!Number.isFinite(n) || n < 1) {
    feedPageSize = 8
    return
  }
  feedPageSize = Math.min(100, Math.max(1, Math.floor(n)))
}

export function getFeedPageSize() {
  return feedPageSize
}

export async function loadPublicConfig() {
  try {
    const { data } = await http.get('/public/config')
    const d = data?.data ?? {}
    setImageCdnBase(typeof d.imageCdnBase === 'string' ? d.imageCdnBase : '')
    const fromApi = typeof d.uploadServerOrigin === 'string' ? d.uploadServerOrigin.trim() : ''
    setUploadServerOrigin(fromApi || inferOriginFromViteApiBase())
    const fps = d.feedPageSize
    if (fps != null && fps !== '') {
      setFeedPageSize(typeof fps === 'number' ? fps : String(fps))
    } else {
      setFeedPageSize(8)
    }
  } catch {
    setImageCdnBase('')
    setUploadServerOrigin(inferOriginFromViteApiBase())
    setFeedPageSize(8)
  }
}

export function fetchAlbums(params = {}) {
  const page = params.page != null ? Number(params.page) : 1
  const pageSize = params.pageSize != null ? Number(params.pageSize) : getFeedPageSize()
  return http.get('/albums', {
    params: {
      page: Number.isFinite(page) && page >= 1 ? Math.floor(page) : 1,
      pageSize: Number.isFinite(pageSize) && pageSize >= 1 ? Math.min(100, Math.floor(pageSize)) : getFeedPageSize()
    }
  })
}

export function fetchAlbumDetail(id) {
  return http.get(`/albums/${id}`)
}

export function reportAlbumView(albumId, visitorId) {
  return http.post('/analytics/track', { albumId, visitorId })
}

/**
 * 访客端展示用：配置了 CDN 时为 CDN 绝对地址；否则为源站绝对地址（若可推断）。
 */
export function toAssetUrl(path) {
  if (!path) return ''
  const p = String(path)
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  const normalized = p.startsWith('/') ? p : `/${p}`
  if (imageCdnBase) {
    return `${imageCdnBase}${normalized}`
  }
  const origin = effectiveUploadOrigin()
  if (origin) {
    return `${origin}${normalized}`
  }
  return normalized
}

/**
 * 直打源站的绝对地址（/uploads/...）；CDN 开启时仍优先用源站，便于预加载算宽高、防盗链回源等。
 */
export function toOriginAssetUrl(path) {
  if (!path) return ''
  const p = String(path)
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  const normalized = p.startsWith('/') ? p : `/${p}`
  const origin = effectiveUploadOrigin()
  if (origin) {
    return `${origin}${normalized}`
  }
  if (imageCdnBase) {
    return `${imageCdnBase}${normalized}`
  }
  return normalized
}
