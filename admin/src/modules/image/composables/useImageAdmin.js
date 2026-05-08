import { onMounted, ref } from 'vue'
import { deleteImage, fetchImages, toggleImageStatus, updateImage } from '../api/imageApi'

export function useImageAdmin() {
  const loading = ref(false)
  const error = ref('')
  const items = ref([])

  const load = async () => {
    loading.value = true
    error.value = ''
    try {
      const { data } = await fetchImages(1, 50)
      items.value = data?.data?.items ?? []
    } catch (err) {
      error.value = err?.message ?? '加载失败'
    } finally {
      loading.value = false
    }
  }

  const editItem = async (item) => {
    try {
      await updateImage(item.id, { title: item.title })
    } catch (err) {
      error.value = err?.message ?? '编辑接口未就绪'
    }
  }

  const removeItem = async (item) => {
    try {
      await deleteImage(item.id)
    } catch (err) {
      error.value = err?.message ?? '删除接口未就绪'
    }
  }

  const toggleStatus = async (item) => {
    const nextOnline = !item.online
    try {
      await toggleImageStatus(item.id, nextOnline)
    } catch (err) {
      error.value = err?.message ?? '上下架接口未就绪'
    }
  }

  onMounted(load)

  return {
    loading,
    error,
    items,
    load,
    editItem,
    removeItem,
    toggleStatus
  }
}
