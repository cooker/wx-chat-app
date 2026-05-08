import { computed, onMounted, ref } from 'vue'
import {
  clearAnalyticsEvents,
  fetchAnalyticsEvents,
  fetchAnalyticsOverview
} from '../api/analyticsApi'

function formatUnixTime(value) {
  const ts = Number(value ?? 0)
  if (!ts) return '--'
  const date = new Date(ts * 1000)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function useAnalyticsDashboard() {
  const loading = ref(false)
  const error = ref('')
  const stats = ref([
    { label: '今日 UV', value: '0' },
    { label: '今日 PV', value: '0' },
    { label: '总 UV', value: '0' }
  ])
  const topAlbums = ref([])
  const todayDevicePv = ref([])
  const events = ref([])
  const page = ref(1)
  const pageSize = ref(20)
  const total = ref(0)

  const load = async () => {
    loading.value = true
    error.value = ''
    try {
      const { data } = await fetchAnalyticsOverview()
      const payload = data?.data ?? {}
      stats.value = [
        { label: '今日 UV', value: String(payload.todayUv ?? 0) },
        { label: '今日 PV', value: String(payload.todayPv ?? 0) },
        { label: '总 UV', value: String(payload.totalUv ?? 0) },
        { label: '最近访问时间', value: formatUnixTime(payload.lastViewedAt) }
      ]
      topAlbums.value = payload.topAlbums ?? []
      todayDevicePv.value = payload.todayDevicePv ?? []
      await loadEvents(page.value)
    } catch (err) {
      error.value = err?.message ?? '统计加载失败'
    } finally {
      loading.value = false
    }
  }

  const loadEvents = async (targetPage = 1) => {
    try {
      const { data } = await fetchAnalyticsEvents(targetPage, pageSize.value)
      events.value = data?.data?.items ?? []
      page.value = Number(data?.data?.page ?? targetPage)
      pageSize.value = Number(data?.data?.pageSize ?? pageSize.value)
      total.value = Number(data?.data?.total ?? 0)
    } catch (err) {
      error.value = err?.message ?? '埋点记录加载失败'
    }
  }

  const clearEvents = async () => {
    try {
      await clearAnalyticsEvents()
      page.value = 1
      await load()
    } catch (err) {
      error.value = err?.message ?? '清空埋点失败'
    }
  }

  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  const hasPrevPage = computed(() => page.value > 1)
  const hasNextPage = computed(() => page.value < totalPages.value)

  const nextPage = async () => {
    if (!hasNextPage.value) return
    await loadEvents(page.value + 1)
  }

  const prevPage = async () => {
    if (!hasPrevPage.value) return
    await loadEvents(page.value - 1)
  }

  onMounted(load)

  return {
    loading,
    error,
    stats,
    topAlbums,
    todayDevicePv,
    events,
    page,
    pageSize,
    total,
    totalPages,
    hasPrevPage,
    hasNextPage,
    load,
    loadEvents,
    nextPage,
    prevPage,
    clearEvents
  }
}
