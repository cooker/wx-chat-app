import { computed, onMounted, ref } from 'vue'
import { fetchAlbumDetail, fetchAlbums, reportAlbumView, toAssetUrl } from '../../../api/images'
import { FEED_PAGE_SIZE, SKELETON_ITEMS } from '../constants/waterfall'

export function useFeedAlbums() {
  const loading = ref(false)
  const error = ref('')
  const list = ref([])
  const allAlbums = ref([])
  const previewVisible = ref(false)
  const activeIndex = ref(0)
  const page = ref(0)
  const hasMore = ref(true)
  const loadingMore = ref(false)
  const previewImages = ref([])
  const previewTitle = ref('')
  const previewDescription = ref('')
  const previewLoading = ref(false)
  const ratioMap = ref({})

  const skeletonItems = ref(SKELETON_ITEMS)

  const waterfallItems = computed(() =>
    list.value.map((item) => ({
      ...item,
      coverDisplay: toAssetUrl(item.coverUrl),
      ratio: ratioMap.value[toAssetUrl(item.coverUrl)] ?? (0.95 + ((item.id % 4) * 0.1)),
      photoText: `${item.imageCount || 0} Photos`
    }))
  )

  const calcItemHeight = (item, width) => Math.round(width * (item.ratio || 1.2))
  const calcSkeletonHeight = (item, width) => Math.round(width * (item.ratio || 1.1))

  const load = async (reset = true) => {
    if (reset) {
      page.value = 0
      hasMore.value = true
      list.value = []
    }
    if (!hasMore.value && !reset) return

    const currentPage = reset ? 0 : page.value
    loading.value = true
    error.value = ''
    try {
      if (reset) {
        const { data } = await fetchAlbums()
        allAlbums.value = data?.data?.items ?? []
        await hydrateImageRatios(allAlbums.value)
      }
      const nextChunk = allAlbums.value.slice(
        currentPage * FEED_PAGE_SIZE,
        (currentPage + 1) * FEED_PAGE_SIZE
      )
      list.value = reset ? nextChunk : [...list.value, ...nextChunk]
      hasMore.value = (currentPage + 1) * FEED_PAGE_SIZE < allAlbums.value.length
      page.value = currentPage + 1
    } catch (err) {
      error.value = err?.message ?? '加载失败'
    } finally {
      loading.value = false
    }
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
    await load(false)
    loadingMore.value = false
  }

  const openPreview = (albumId) => {
    const album = list.value.find((item) => item.id === albumId)
    if (!album) return
    activeIndex.value = 0
    previewTitle.value = album.title
    previewDescription.value = album.description
    previewImages.value = album.coverUrl ? [{ url: album.coverUrl, originalName: album.title }] : []
    previewLoading.value = true
    previewVisible.value = true
    reportAlbumView(album.id, getVisitorId()).catch(() => {})
    fetchAlbumDetail(album.id)
      .then(({ data }) => {
        const images = data?.data?.images ?? []
        if (images.length > 0) previewImages.value = images
      })
      .finally(() => {
        previewLoading.value = false
      })
  }

  const getVisitorId = () => {
    const key = 'wx-chat-visitor-id'
    const current = window.localStorage.getItem(key)
    if (current) return current
    const created = `visitor-${crypto.randomUUID()}`
    window.localStorage.setItem(key, created)
    return created
  }

  const closePreview = () => {
    previewVisible.value = false
  }

  const hydrateImageRatios = async (albums) => {
    const tasks = (albums || []).map(async (album) => {
      const url = toAssetUrl(album.coverUrl)
      if (!url || ratioMap.value[url]) return
      const ratio = await readImageRatio(url)
      if (ratio) ratioMap.value[url] = ratio
    })
    await Promise.all(tasks)
  }

  const readImageRatio = (url) =>
    new Promise((resolve) => {
      const img = new Image()
      img.onload = () => resolve(img.naturalWidth > 0 ? img.naturalHeight / img.naturalWidth : null)
      img.onerror = () => resolve(null)
      img.src = url
    })

  onMounted(load)

  return {
    loading,
    error,
    list,
    previewVisible,
    activeIndex,
    hasMore,
    loadingMore,
    previewImages,
    previewTitle,
    previewDescription,
    previewLoading,
    skeletonItems,
    waterfallItems,
    calcItemHeight,
    calcSkeletonHeight,
    load,
    loadMore,
    openPreview,
    closePreview
  }
}
