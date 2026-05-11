import http from '../../../api/http.js'

export function fetchAnalyticsOverview() {
  return http.get('/analytics/overview')
}

export function fetchAnalyticsEvents(page = 1, pageSize = 15) {
  return http.get('/analytics/events', { params: { page, pageSize } })
}

/** 今日（服务器时区）埋点中去重后的 IP 列表 */
export function fetchTodayDistinctIps() {
  return http.get('/analytics/today-ips')
}

export function clearAnalyticsEvents() {
  return http.delete('/analytics/events')
}
