import { computed, onMounted, ref } from 'vue'
import {
  createAlbum,
  deleteAlbum,
  fetchAlbumDetail,
  fetchAlbums,
  updateAlbum,
  uploadAlbumImage
} from '../api/albumApi'

export function useAlbumAdmin() {
  const createFolder = () => `album-${Date.now()}`
  const loading = ref(false)
  const error = ref('')
  const albums = ref([])
  const page = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const form = ref({
    id: null,
    title: '',
    description: '',
    coverUrl: '',
    imageFolder: createFolder()
  })
  const uploadedImages = ref([])
  const uploading = ref(false)

  const isEdit = computed(() => form.value.id !== null)

  const loadAlbums = async (targetPage = page.value) => {
    loading.value = true
    error.value = ''
    try {
      const { data } = await fetchAlbums(targetPage, pageSize.value)
      const payload = data?.data ?? {}
      albums.value = payload.items ?? []
      page.value = Number(payload.page ?? targetPage)
      pageSize.value = Number(payload.pageSize ?? pageSize.value)
      total.value = Number(payload.total ?? 0)
    } catch (err) {
      error.value = err?.message ?? '加载相册失败'
    } finally {
      loading.value = false
    }
  }

  const uploadFiles = async (files) => {
    if (!files || files.length === 0) return
    uploading.value = true
    try {
      for (const file of files) {
        const { data } = await uploadAlbumImage(file, form.value.imageFolder)
        const image = data?.data
        if (!image) continue
        if (!uploadedImages.value.find((item) => item.id === image.id)) {
          uploadedImages.value.push(image)
        }
        if (!form.value.coverUrl) {
          form.value.coverUrl = image.url
        }
      }
    } catch (err) {
      error.value = err?.message ?? '上传失败'
    } finally {
      uploading.value = false
    }
  }

  const editAlbum = async (albumId) => {
    try {
      const { data } = await fetchAlbumDetail(albumId)
      const detail = data?.data
      if (!detail) return
      form.value = {
        id: detail.id,
        title: detail.title,
        description: detail.description,
        coverUrl: detail.coverUrl ?? '',
        imageFolder: detail.imageFolder || createFolder()
      }
      uploadedImages.value = detail.images ?? []
    } catch (err) {
      error.value = err?.message ?? '加载相册详情失败'
    }
  }

  const resetForm = () => {
    form.value = {
      id: null,
      title: '',
      description: '',
      coverUrl: '',
      imageFolder: createFolder()
    }
    uploadedImages.value = []
  }

  const removeImage = (url) => {
    uploadedImages.value = uploadedImages.value.filter((img) => img.url !== url)
    if (form.value.coverUrl === url) {
      form.value.coverUrl = uploadedImages.value[0]?.url ?? ''
    }
  }

  const saveAlbum = async () => {
    error.value = ''
    const payload = {
      title: form.value.title,
      description: form.value.description,
      coverUrl: form.value.coverUrl,
      imageFolder: form.value.imageFolder
    }
    try {
      if (isEdit.value) {
        await updateAlbum(form.value.id, payload)
      } else {
        await createAlbum(payload)
      }
      await loadAlbums(page.value)
      resetForm()
    } catch (err) {
      error.value = err?.message ?? '保存相册失败'
    }
  }

  const removeAlbum = async (albumId) => {
    try {
      await deleteAlbum(albumId)
      const maxPage = Math.max(1, Math.ceil(Math.max(0, total.value - 1) / pageSize.value))
      await loadAlbums(Math.min(page.value, maxPage))
    } catch (err) {
      error.value = err?.message ?? '删除相册失败'
    }
  }

  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  const hasPrevPage = computed(() => page.value > 1)
  const hasNextPage = computed(() => page.value < totalPages.value)

  const prevPage = async () => {
    if (!hasPrevPage.value) return
    await loadAlbums(page.value - 1)
  }

  const nextPage = async () => {
    if (!hasNextPage.value) return
    await loadAlbums(page.value + 1)
  }

  onMounted(loadAlbums)

  return {
    loading,
    error,
    albums,
    page,
    pageSize,
    total,
    totalPages,
    hasPrevPage,
    hasNextPage,
    form,
    uploadedImages,
    uploading,
    isEdit,
    loadAlbums,
    uploadFiles,
    editAlbum,
    removeImage,
    saveAlbum,
    removeAlbum,
    prevPage,
    nextPage,
    resetForm
  }
}
