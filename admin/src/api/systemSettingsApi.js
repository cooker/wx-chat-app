import http from './http'
import { setImageCdnBase } from '../utils/assetUrl'

export async function loadPublicConfig() {
  try {
    const { data } = await http.get('/public/config')
    const base = data?.data?.imageCdnBase
    setImageCdnBase(typeof base === 'string' ? base : '')
  } catch {
    setImageCdnBase('')
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
