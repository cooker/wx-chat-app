import { ref } from 'vue'
import { fetchTodayDistinctIps } from '../../../analytics/api/analyticsApi.js'
import { fetchIpPortraitBrief } from '../api/ipPortraitApi.js'
import { extractCityFromBrief } from '../utils/extractCityFromBrief.js'

export function useTodayIpPortraits() {
  const loading = ref(false)
  const error = ref('')
  const rows = ref([])
  /** 是否已至少完成一次「拉列表」请求（用于区分未加载与加载结果为空） */
  const fetchDone = ref(false)

  const load = async () => {
    loading.value = true
    error.value = ''
    rows.value = []
    fetchDone.value = false
    try {
      const { data: listRes } = await fetchTodayDistinctIps()
      if (listRes?.code !== 0) {
        error.value = listRes?.message || '加载今日 IP 失败'
        return
      }
      const ips = Array.isArray(listRes?.data?.ips) ? listRes.data.ips : []
      rows.value = ips.map((ip) => ({ ip, city: '', error: '' }))
      for (let i = 0; i < rows.value.length; i++) {
        const ip = rows.value[i].ip
        try {
          const { data: briefRes } = await fetchIpPortraitBrief(ip)
          if (briefRes?.code !== 0) {
            rows.value[i] = { ...rows.value[i], error: briefRes?.message || '画像接口错误' }
          } else {
            rows.value[i] = {
              ...rows.value[i],
              city: extractCityFromBrief(briefRes?.data)
            }
          }
        } catch (err) {
          rows.value[i] = {
            ...rows.value[i],
            error: err?.response?.data?.message ?? err?.message ?? '查询失败'
          }
        }
      }
    } catch (err) {
      error.value = err?.response?.data?.message ?? err?.message ?? '加载今日 IP 失败'
    } finally {
      loading.value = false
      fetchDone.value = true
    }
  }

  return {
    loading,
    error,
    rows,
    fetchDone,
    load
  }
}
