<script setup>
import { VirtualWaterfall } from '@lhlyu/vue-virtual-waterfall'
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import AlbumCard from '../modules/feed/components/AlbumCard.vue'
import LazyImage from '../components/LazyImage.vue'
import { WATERFALL_LAYOUT } from '../modules/feed/constants/waterfall'
import { useFeedAlbums } from '../modules/feed/composables/useFeedAlbums'
import { fetchHotAlbums, getHotAlbumSize, getSiteTopDescription, getSiteTopTitle, toAssetUrl } from '../api/images'

const PULL_THRESHOLD = 56
const PULL_MAX = 96
const PULL_DAMP = 0.52

const mainRef = ref(null)
const sentinelRef = ref(null)
const keyword = ref('')
const hotLoading = ref(false)
const hotAlbums = ref([])
const router = useRouter()
let loadMoreObserver = null

const SCROLL_NEAR_BOTTOM_DESKTOP = 240
const SCROLL_NEAR_BOTTOM_TOUCH = 380

function preferTouchUi() {
  try {
    return window.matchMedia('(pointer: coarse), (hover: none)').matches
  } catch {
    return false
  }
}

function scrollNearBottomThreshold() {
  return preferTouchUi() ? SCROLL_NEAR_BOTTOM_TOUCH : SCROLL_NEAR_BOTTOM_DESKTOP
}

function loadMoreIoRootMargin() {
  return preferTouchUi() ? '80px 0px min(52vh, 520px) 0px' : '400px 0px 220px 0px'
}

const pullOffset = ref(0)
const pullActive = ref(false)
let touchStartY = 0

const {
  loading,
  refreshing,
  error,
  list,
  hasMore,
  loadingMore,
  skeletonItems,
  waterfallItems,
  calcItemHeight,
  calcSkeletonHeight,
  load,
  loadMore
} = useFeedAlbums()

const heroTitle = computed(() => {
  const t = getSiteTopTitle()?.trim()
  return t || '相册'
})
const heroSubtitle = computed(() => {
  const t = getSiteTopDescription()?.trim()
  return t || '记录生活，珍藏美好'
})
const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const searching = computed(() => normalizedKeyword.value.length > 0)
const waterfallModeKey = computed(() => (searching.value ? 'search' : 'default'))

const dedupeItems = (items) => {
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

const recommendAlbums = computed(() => {
  const base = dedupeItems(waterfallItems.value)
  const query = normalizedKeyword.value
  if (!query) return base
  return base.filter((item) =>
    [item.title, item.description].join(' ').toLowerCase().includes(query)
  )
})

async function loadHotAlbums() {
  hotLoading.value = true
  try {
    const size = getHotAlbumSize()
    const { data } = await fetchHotAlbums({ size })
    const items = Array.isArray(data?.data?.items) ? data.data.items : []
    hotAlbums.value = items.map((item) => ({
      ...item,
      coverDisplay: toAssetUrl(item.coverUrl),
      photoText: `${item.imageCount || 0}张`
    }))
  } finally {
    hotLoading.value = false
  }
}

async function refreshAll() {
  await Promise.allSettled([loadHotAlbums(), load(true)])
}

const pullHint = computed(() => {
  if (refreshing.value) return '刷新中…'
  if (pullOffset.value >= PULL_THRESHOLD) return '松开刷新'
  return '下拉刷新'
})

function scrollTop() {
  return window.scrollY || document.documentElement.scrollTop || 0
}

function canPullRefresh() {
  return !error.value && !loading.value && !loadingMore.value && !refreshing.value
}

function canScrollLoadMore() {
  if (searching.value) return false
  return (
    !error.value && !loading.value && !loadingMore.value && !refreshing.value && hasMore.value && list.value.length > 0
  )
}

function checkScrollLoadMore() {
  if (!canScrollLoadMore()) return
  const doc = document.documentElement
  const body = document.body
  const scrollY = window.scrollY || doc.scrollTop || body.scrollTop || 0
  const viewH =
    preferTouchUi() && window.visualViewport?.height
      ? window.visualViewport.height
      : window.innerHeight || doc.clientHeight || 0
  const fullH = Math.max(
    body.scrollHeight,
    body.offsetHeight,
    doc.scrollHeight,
    doc.offsetHeight,
    doc.clientHeight
  )
  if (scrollY + viewH >= fullH - scrollNearBottomThreshold()) {
    void loadMore()
  }
}

let scrollLoadRaf = 0
function scheduleScrollLoadCheck() {
  if (scrollLoadRaf) return
  scrollLoadRaf = requestAnimationFrame(() => {
    scrollLoadRaf = 0
    checkScrollLoadMore()
  })
}

function onTouchMoveScrollHint() {
  scheduleScrollLoadCheck()
}

function bindScrollLoadListeners() {
  window.addEventListener('scroll', scheduleScrollLoadCheck, { passive: true, capture: true })
  document.addEventListener('scroll', scheduleScrollLoadCheck, { passive: true, capture: true })
  window.visualViewport?.addEventListener('scroll', scheduleScrollLoadCheck, { passive: true })
  window.visualViewport?.addEventListener('resize', scheduleScrollLoadCheck, { passive: true })
  window.addEventListener('touchmove', onTouchMoveScrollHint, { passive: true })
}

function unbindScrollLoadListeners() {
  window.removeEventListener('scroll', scheduleScrollLoadCheck, { capture: true })
  document.removeEventListener('scroll', scheduleScrollLoadCheck, { capture: true })
  window.visualViewport?.removeEventListener('scroll', scheduleScrollLoadCheck)
  window.visualViewport?.removeEventListener('resize', scheduleScrollLoadCheck)
  window.removeEventListener('touchmove', onTouchMoveScrollHint)
}

function disconnectLoadMoreObserver() {
  loadMoreObserver?.disconnect()
  loadMoreObserver = null
}

watch(
  [loading, searching],
  async () => {
    disconnectLoadMoreObserver()
    if (loading.value || list.value.length === 0 || searching.value) return
    await nextTick()
    if (!sentinelRef.value) return
    loadMoreObserver = new IntersectionObserver(
      (entries) => {
        if (!searching.value && entries.some((entry) => entry.isIntersecting)) {
          void loadMore()
        }
      },
      { root: null, rootMargin: loadMoreIoRootMargin(), threshold: 0 }
    )
    loadMoreObserver.observe(sentinelRef.value)
  },
  { flush: 'post' }
)

function goAlbumDetail(albumId) {
  router.push({ name: 'album-detail', params: { id: String(albumId) } })
}

function onTouchStart(event) {
  if (!canPullRefresh()) return
  if (scrollTop() > 8) return
  const y = event.touches?.[0]?.clientY
  if (y == null) return
  pullActive.value = true
  touchStartY = y
}

function onTouchMove(event) {
  if (!pullActive.value || !canPullRefresh()) return
  if (scrollTop() > 8) {
    pullOffset.value = 0
    return
  }
  const y = event.touches?.[0]?.clientY
  if (y == null) return
  const distance = y - touchStartY
  if (distance <= 0) {
    pullOffset.value = 0
    return
  }
  pullOffset.value = Math.min(PULL_MAX, distance * PULL_DAMP)
  if (pullOffset.value > 4) event.preventDefault()
}

function onTouchEnd() {
  if (!pullActive.value) return
  pullActive.value = false
  const shouldRefresh = pullOffset.value >= PULL_THRESHOLD && canPullRefresh()
  pullOffset.value = 0
  if (!shouldRefresh) return
  void refreshAll().finally(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function bindPullListeners() {
  if (!mainRef.value) return
  mainRef.value.addEventListener('touchstart', onTouchStart, { passive: true })
  mainRef.value.addEventListener('touchmove', onTouchMove, { passive: false })
  mainRef.value.addEventListener('touchend', onTouchEnd, { passive: true })
  mainRef.value.addEventListener('touchcancel', onTouchEnd, { passive: true })
}

function unbindPullListeners() {
  if (!mainRef.value) return
  mainRef.value.removeEventListener('touchstart', onTouchStart)
  mainRef.value.removeEventListener('touchmove', onTouchMove)
  mainRef.value.removeEventListener('touchend', onTouchEnd)
  mainRef.value.removeEventListener('touchcancel', onTouchEnd)
}

onMounted(() => {
  bindPullListeners()
  bindScrollLoadListeners()
  void loadHotAlbums()
})

onBeforeUnmount(() => {
  disconnectLoadMoreObserver()
  unbindPullListeners()
  unbindScrollLoadListeners()
  if (scrollLoadRaf) cancelAnimationFrame(scrollLoadRaf)
  scrollLoadRaf = 0
})
</script>

<template>
  <main ref="mainRef" class="feed-page">
    <div
      class="pull-refresh-track"
      :class="{ 'pull-refresh-track--active': pullOffset > 2 || refreshing }"
      :style="{ height: `${refreshing ? PULL_THRESHOLD : pullOffset}px` }"
    >
      <span class="pull-refresh-hint">{{ pullHint }}</span>
    </div>

    <section class="feed-hero">
      <div>
        <h1 class="feed-hero-title">{{ heroTitle }}</h1>
        <p class="feed-hero-subtitle">{{ heroSubtitle }}</p>
      </div>
      <button type="button" class="feed-icon-btn" :disabled="loading || refreshing" @click="refreshAll">
        {{ refreshing ? '...' : '↻' }}
      </button>
    </section>

    <section class="feed-search">
      <input
        v-model="keyword"
        type="search"
        class="feed-search-input"
        placeholder="搜索相册、地点、人物..."
        autocomplete="off"
      />
    </section>

    <section class="feed-section">
      <div class="feed-section-head">
        <h2>热门相册</h2>
      </div>
      <div v-if="hotLoading && hotAlbums.length === 0" class="hot-empty">热门加载中...</div>
      <div v-else-if="hotAlbums.length === 0" class="hot-empty">暂无热门相册</div>
      <div v-else class="hot-strip">
        <button
          v-for="album in hotAlbums"
          :key="`hot-${album.id}`"
          type="button"
          class="hot-card"
          @click="goAlbumDetail(album.id)"
        >
          <LazyImage layout="fill" img-class="hot-card-cover" :src="album.coverDisplay" :alt="album.title" />
          <div class="hot-card-overlay">
            <strong>{{ album.title }}</strong>
            <span>{{ album.photoText }}</span>
          </div>
        </button>
      </div>
    </section>

    <section class="feed-section">
      <div class="feed-section-head">
        <h2>推荐</h2>
        <button type="button" class="feed-link-btn" :disabled="refreshing" @click="refreshAll">换一换</button>
      </div>

      <div v-if="error" class="error-wrap">
        <p class="error">{{ error }}</p>
        <button type="button" class="feed-link-btn" @click="refreshAll">重试</button>
      </div>

      <template v-else>
        <section v-if="loading && !list.length" class="waterfall-container">
          <VirtualWaterfall
            :items="skeletonItems"
            row-key="id"
            :item-min-width="WATERFALL_LAYOUT.itemMinWidth"
            :min-column-count="WATERFALL_LAYOUT.minColumnCount"
            :max-column-count="WATERFALL_LAYOUT.maxColumnCount"
            :gap="WATERFALL_LAYOUT.gap"
            :padding="WATERFALL_LAYOUT.padding"
            :calc-item-height="calcSkeletonHeight"
          >
            <template #default>
              <article class="card skeleton-card">
                <div class="skeleton-cover"></div>
              </article>
            </template>
          </VirtualWaterfall>
        </section>
        <div v-else-if="!recommendAlbums.length && !loading" class="empty">暂无匹配内容</div>
        <section v-else class="waterfall-container">
          <div v-if="refreshing" class="feed-refresh-bar" aria-live="polite">正在更新推荐...</div>
          <VirtualWaterfall
            :key="waterfallModeKey"
            :items="recommendAlbums"
            row-key="id"
            :item-min-width="WATERFALL_LAYOUT.itemMinWidth"
            :min-column-count="WATERFALL_LAYOUT.minColumnCount"
            :max-column-count="WATERFALL_LAYOUT.maxColumnCount"
            :gap="WATERFALL_LAYOUT.gap"
            :padding="WATERFALL_LAYOUT.padding"
            :calc-item-height="calcItemHeight"
          >
            <template #default="{ item }">
              <AlbumCard :item="item" @select="goAlbumDetail" />
            </template>
          </VirtualWaterfall>
          <div ref="sentinelRef" class="feed-scroll-sentinel" aria-hidden="true" />
        </section>
      </template>
    </section>

    <div v-if="!error && list.length > 0" class="feed-status">
      <button
        v-if="hasMore"
        type="button"
        class="feed-load-more-btn"
        :disabled="loadingMore || refreshing"
        @click="loadMore"
      >
        {{ loadingMore ? '加载中…' : '加载更多' }}
      </button>
      <span v-else class="feed-status-end">已经到底啦</span>
    </div>

  </main>
</template>
