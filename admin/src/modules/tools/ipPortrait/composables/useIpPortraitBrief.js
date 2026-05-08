import { ref } from 'vue'
import { fetchIpPortraitBrief } from '../api/ipPortraitApi'

export function useIpPortraitBrief() {
  const loading = ref(false)
  const error = ref('')
  const result = ref(null)

  const query = async (ip) => {
    loading.value = true
    error.value = ''
    result.value = null
    try {
      const { data } = await fetchIpPortraitBrief(ip)
      result.value = data?.data ?? null
      if (data?.code !== 0) {
        error.value = data?.message || '请求失败'
      }
    } catch (err) {
      error.value = err?.response?.data?.message ?? err?.message ?? '查询失败'
      result.value = null
    } finally {
      loading.value = false
    }
  }

  const clear = () => {
    error.value = ''
    result.value = null
  }

  return {
    loading,
    error,
    result,
    query,
    clear
  }
}
