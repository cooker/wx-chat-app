import http from '../../../api/http.js'

export function fetchAnalyticsOverview() {
  return http.get('/analytics/overview')
}

export function fetchAnalyticsEvents(page = 1, pageSize = 20) {
  return http.get('/analytics/events', { params: { page, pageSize } })
}

export function clearAnalyticsEvents() {
  return http.delete('/analytics/events')
}
