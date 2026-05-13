let imageCdnBase = ''
let uploadServerOrigin = ''

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

export function getImageCdnBase() {
  return imageCdnBase
}

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
