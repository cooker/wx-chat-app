import http from '../../../../api/http.js'

/**
 * 调用后端代理的奇符 IP 画像 brief-info（管理端工具）。
 * @param {string} ip
 */
export function fetchIpPortraitBrief(ip) {
  return http.get('/tools/ip-portrait', { params: { ip } })
}
