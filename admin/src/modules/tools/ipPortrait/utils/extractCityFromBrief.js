/**
 * 从奇符 brief-info 原始 JSON 中提取城市（兼容多种嵌套与大小写字段名）。
 * @param {unknown} payload - 通常为接口返回的 data 对象
 * @returns {string}
 */
function readCityString(node) {
  if (!node || typeof node !== 'object') return ''
  const v = node.city ?? node.City
  return typeof v === 'string' && v.trim() ? v.trim() : ''
}

function findFirstCityString(node, depth) {
  if (!node || typeof node !== 'object' || depth > 10) return ''
  const direct = readCityString(node)
  if (direct) return direct
  for (const v of Object.values(node)) {
    if (v && typeof v === 'object') {
      const inner = findFirstCityString(v, depth + 1)
      if (inner) return inner
    }
  }
  return ''
}

export function extractCityFromBrief(payload) {
  return findFirstCityString(payload, 0)
}
