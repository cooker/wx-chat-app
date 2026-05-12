let imageCdnBase = ''

export function setImageCdnBase(value) {
  if (typeof value !== 'string' || !value.trim()) {
    imageCdnBase = ''
    return
  }
  let b = value.trim()
  while (b.endsWith('/')) {
    b = b.slice(0, -1)
  }
  imageCdnBase = b
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
  return normalized
}
