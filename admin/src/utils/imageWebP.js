/** 与后端一致：原图最大 10MB；上传前在浏览器内转为 WebP。 */
export const MAX_IMAGE_UPLOAD_BYTES = 10 * 1024 * 1024

const ALLOWED_NAME = /\.(jpe?g|jfif|png|webp)$/i

export function validateImageFileForUpload(file) {
  if (!file?.name?.match(ALLOWED_NAME)) {
    throw new Error('仅支持 jpg、jpeg、jfif、png、webp')
  }
  if (file.size > MAX_IMAGE_UPLOAD_BYTES) {
    throw new Error('单张图片不能超过 10MB')
  }
}

function fitWithinMaxSide(imgW, imgH, maxSide) {
  const m = Math.max(imgW, imgH)
  if (m <= maxSide) {
    return { w: imgW, h: imgH }
  }
  const scale = maxSide / m
  return { w: Math.round(imgW * scale), h: Math.round(imgH * scale) }
}

/**
 * @param {File} file
 * @param {{ quality?: number, maxSide?: number }} [options]
 * @returns {Promise<File>} 文件名 *.webp，type image/webp
 */
export function convertImageFileToWebP(file, options = {}) {
  const quality = typeof options.quality === 'number' ? options.quality : 0.9
  const maxSide = typeof options.maxSide === 'number' ? options.maxSide : 8192

  validateImageFileForUpload(file)

  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      try {
        const { w, h } = fitWithinMaxSide(img.naturalWidth, img.naturalHeight, maxSide)
        if (w < 1 || h < 1) {
          reject(new Error('图片尺寸无效'))
          return
        }
        const canvas = document.createElement('canvas')
        canvas.width = w
        canvas.height = h
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('无法创建画布'))
          return
        }
        const lower = file.name.toLowerCase()
        if (lower.match(/\.(jpe?g|jfif)$/)) {
          ctx.fillStyle = '#ffffff'
          ctx.fillRect(0, 0, w, h)
        }
        ctx.drawImage(img, 0, 0, w, h)
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(
                new Error(
                  '当前浏览器无法导出 WebP，请使用 Chrome / Edge / Firefox 较新版本或 Safari 14+'
                )
              )
              return
            }
            const base = file.name.replace(/\.[^/.]+$/, '') || 'image'
            resolve(new File([blob], `${base}.webp`, { type: 'image/webp' }))
          },
          'image/webp',
          quality
        )
      } catch (e) {
        reject(e instanceof Error ? e : new Error(String(e)))
      }
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('无法读取图片文件'))
    }
    img.src = url
  })
}
