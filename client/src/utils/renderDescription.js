import { marked } from 'marked'

const HTML_LIKE_RE = /<[a-z][\s\S]*>/i

/**
 * 相册描述 → 可安全展示的 HTML（Markdown 或已是 HTML 的字符串）
 */
export function renderAlbumDescription(raw = '') {
  const text = String(raw ?? '').trim()
  if (!text) return ''
  if (HTML_LIKE_RE.test(text)) {
    return text
  }
  return marked.parse(text, { breaks: true })
}
