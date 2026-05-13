import { computed, onMounted, ref } from 'vue'
import {
  fetchAlbumDetail,
  fetchAlbums,
  reportAlbumView,
  toAssetUrl,
  toOriginAssetUrl,
  getFeedPageSize
} from '../../../api/images'
import { SKELETON_ITEMS } from '../constants/waterfall'

export function useFeedAlbums() {
  const loading = ref(false)
  const refreshing = ref(false)
  const error = ref('')
  const list = ref([])
  const previewVisible = ref(false)
  const activeIndex = ref(0)
  const hasMore = ref(true)
  const loadingMore = ref(false)
  const previewImages = ref([])
  const previewTitle = ref('')
  const previewDescription = ref('')
  const previewLoading = ref(false)
  const ratioMap = ref({})
  const nextApiPage = ref(1)

  const skeletonItems = ref(SKELETON_ITEMS)

  const waterfallItems = computed(() =>
    list.value.map((item) => ({
      ...item,
      coverDisplay: toAssetUrl(item.coverUrl),
      ratio: ratioMap.value[toAssetUrl(item.coverUrl)] ?? (0.95 + ((item.id % 4) * 0.1)),
      photoText: `${item.imageCount || 0}张`,
      authorText: item.createdBy || item.ownerName || '匿名用户',
      likeText: item.views != null ? String(item.views) : '--'
    }))
  )

  const calcItemHeight = (item, width) => Math.round(width * (item.ratio || 1.2))
  const calcSkeletonHeight = (item, width) => Math.round(width * (item.ratio || 1.1))

  const dedupeById = (items) => {
    const map = new Map()
    for (const item of items || []) {
      if (!item) continue
      const key =
        item.id != null && item.id !== ''
          ? `id:${String(item.id)}`
          : `fallback:${String(item.coverUrl || '')}::${String(item.title || '')}`
      map.set(key, item)
    }
    return [...map.values()]
  }

  const load = async (reset = true) => {
    if (!reset) {
      // 注意：loadMore 会先置 loadingMore 再调用 load(false)，此处不能判断 loadingMore
      if (!hasMore.value || loading.value || refreshing.value) return
    } else {
      if (loading.value || refreshing.value) return
      nextApiPage.value = 1
      hasMore.value = true
      if (list.value.length === 0) {
        loading.value = true
      } else {
        refreshing.value = true
      }
    }

    error.value = ''
    const apiPage = reset ? 1 : nextApiPage.value
    const pageSize = getFeedPageSize()

    try {
      const { data } = await fetchAlbums({ page: apiPage, pageSize })
      const payload = data?.data ?? {}
      const items = Array.isArray(payload.items) ? payload.items : []
      const totalNum = Number(payload.total)
      const totalValid = Number.isFinite(totalNum) && totalNum >= 0

      if (reset) {
        list.value = dedupeById(items)
        nextApiPage.value = 2
      } else if (items.length > 0) {
        list.value = dedupeById([...list.value, ...items])
        nextApiPage.value = apiPage + 1
      }

      if (!reset && items.length === 0) {
        hasMore.value = false
      } else if (totalValid) {
        hasMore.value = list.value.length < totalNum
      } else {
        // total 异常时：满页则认为可能还有下一页
        hasMore.value = items.length >= pageSize
      }

      await hydrateImageRatios(items)
    } catch (err) {
      error.value = err?.message ?? '加载失败'
    } finally {
      if (reset) {
        loading.value = false
        refreshing.value = false
      }
    }
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value || loading.value || refreshing.value) return
    loadingMore.value = true
    try {
      await load(false)
    } finally {
      loadingMore.value = false
    }
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
      const displayUrl = toAssetUrl(album.coverUrl)
      if (!displayUrl || ratioMap.value[displayUrl]) return
      const loadUrl = toOriginAssetUrl(album.coverUrl)
      const ratio = await readImageRatio(loadUrl || displayUrl)
      if (ratio) ratioMap.value[displayUrl] = ratio
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
    refreshing,
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
