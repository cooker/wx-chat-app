import http from './http'
import { setImageCdnBase, setUploadServerOrigin } from '../utils/assetUrl'

function inferOriginFromViteApiBase() {
  const base = import.meta.env.VITE_API_BASE || '/api'
  if (typeof base !== 'string' || !/^https?:\/\//i.test(base)) {
    return ''
  }
  try {
    let o = new URL(base).origin
    while (o.endsWith('/')) {
      o = o.slice(0, -1)
    }
    return o
  } catch {
    return ''
  }
}

export async function loadPublicConfig() {
  try {
    const { data } = await http.get('/public/config')
    const d = data?.data ?? {}
    setImageCdnBase(typeof d.imageCdnBase === 'string' ? d.imageCdnBase : '')
    const fromApi = typeof d.uploadServerOrigin === 'string' ? d.uploadServerOrigin.trim() : ''
    setUploadServerOrigin(fromApi || inferOriginFromViteApiBase())
  } catch {
    setImageCdnBase('')
    setUploadServerOrigin(inferOriginFromViteApiBase())
  }
}

export async function fetchImageCdnSetting() {
  const { data } = await http.get('/admin/settings/image-cdn')
  return data?.data ?? { imageCdnBase: '' }
}

export async function saveImageCdnSetting(imageCdnBase) {
  const { data } = await http.put('/admin/settings/image-cdn', { imageCdnBase })
  const out = data?.data ?? {}
  if (typeof out.imageCdnBase === 'string') {
    setImageCdnBase(out.imageCdnBase)
  }
  return out
}

export async function fetchFeedPageSetting() {
  const { data } = await http.get('/admin/settings/feed-page')
  return data?.data ?? { feedPageSize: 8 }
}

export async function saveFeedPageSetting(feedPageSize) {
  const { data } = await http.put('/admin/settings/feed-page', { feedPageSize })
  return data?.data ?? {}
}

export async function fetchHotAlbumSizeSetting() {
  const { data } = await http.get('/admin/settings/hot-album-size')
  return data?.data ?? { hotAlbumSize: 8 }
}

export async function saveHotAlbumSizeSetting(hotAlbumSize) {
  const { data } = await http.put('/admin/settings/hot-album-size', { hotAlbumSize })
  return data?.data ?? {}
}
