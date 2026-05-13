<script setup>
import { VirtualWaterfall } from '@lhlyu/vue-virtual-waterfall'
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import FeedHeader from '../modules/feed/components/FeedHeader.vue'
import AlbumViewer from '../modules/feed/components/AlbumViewer.vue'
import AlbumCard from '../modules/feed/components/AlbumCard.vue'
import { WATERFALL_LAYOUT } from '../modules/feed/constants/waterfall'
import { useFeedAlbums } from '../modules/feed/composables/useFeedAlbums'

const PULL_THRESHOLD = 56
const PULL_MAX = 96
const PULL_DAMP = 0.52

const mainRef = ref(null)
const sentinelRef = ref(null)
let loadMoreObserver = null

/** 桌面端滚轮：视口接近文档底部时加载更多 */
const SCROLL_NEAR_BOTTOM_PX = 240
let scrollLoadRaf = 0

const pullOffset = ref(0)
const pullActive = ref(false)
let touchStartY = 0

const {
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
} = useFeedAlbums()

function scrollTop() {
  return window.scrollY || document.documentElement.scrollTop || 0
}

function canPullRefresh() {
  return (
    !previewVisible.value &&
    !error.value &&
    !loading.value &&
    !loadingMore.value &&
    !refreshing.value
  )
}

const pullHint = computed(() => {
  if (refreshing.value) return '刷新中…'
  if (pullOffset.value >= PULL_THRESHOLD) return '松开刷新'
  return '下拉刷新'
})

function canScrollLoadMore() {
  return (
    !previewVisible.value &&
    !error.value &&
    !loading.value &&
    !loadingMore.value &&
    !refreshing.value &&
    hasMore.value &&
    list.value.length > 0
  )
}

function checkScrollLoadMore() {
  if (!canScrollLoadMore()) return
  const doc = document.documentElement
  const body = document.body
  const scrollTop = window.scrollY || doc.scrollTop || body.scrollTop || 0
  const viewH = window.innerHeight || doc.clientHeight || 0
  const fullH = Math.max(
    body.scrollHeight,
    body.offsetHeight,
    doc.scrollHeight,
    doc.offsetHeight,
    doc.clientHeight
  )
  if (scrollTop + viewH >= fullH - SCROLL_NEAR_BOTTOM_PX) {
    void loadMore()
  }
}

function onWindowScrollLoadMore() {
  if (scrollLoadRaf) return
  scrollLoadRaf = requestAnimationFrame(() => {
    scrollLoadRaf = 0
    checkScrollLoadMore()
  })
}

function disconnectLoadMoreObserver() {
  loadMoreObserver?.disconnect()
  loadMoreObserver = null
}

watch(
  [previewVisible, loading],
  async () => {
    disconnectLoadMoreObserver()
    if (previewVisible.value || loading.value || list.value.length === 0) return
    await nextTick()
    const target = sentinelRef.value
    if (!target) return
    loadMoreObserver = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          loadMore()
        }
      },
      { root: null, rootMargin: '400px 0px 200px 0px', threshold: 0 }
    )
    loadMoreObserver.observe(target)
  },
  { flush: 'post' }
)

function onTouchStart(e) {
  if (!canPullRefresh()) return
  if (scrollTop() > 8) return
  const y = e.touches?.[0]?.clientY
  if (y == null) return
  pullActive.value = true
  touchStartY = y
}

function onTouchMove(e) {
  if (!pullActive.value || !canPullRefresh()) return
  if (scrollTop() > 8) {
    pullOffset.value = 0
    return
  }
  const y = e.touches?.[0]?.clientY
  if (y == null) return
  const dy = y - touchStartY
  if (dy <= 0) {
    pullOffset.value = 0
    return
  }
  pullOffset.value = Math.min(PULL_MAX, dy * PULL_DAMP)
  if (pullOffset.value > 4) {
    e.preventDefault()
  }
}

function onTouchEnd() {
  if (!pullActive.value) return
  pullActive.value = false
  const shouldRefresh = pullOffset.value >= PULL_THRESHOLD && canPullRefresh()
  pullOffset.value = 0
  if (!shouldRefresh) return
  void load(true).finally(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function bindPullListeners() {
  const el = mainRef.value
  if (!el) return
  el.addEventListener('touchstart', onTouchStart, { passive: true })
  el.addEventListener('touchmove', onTouchMove, { passive: false })
  el.addEventListener('touchend', onTouchEnd, { passive: true })
  el.addEventListener('touchcancel', onTouchEnd, { passive: true })
}

function unbindPullListeners() {
  const el = mainRef.value
  if (!el) return
  el.removeEventListener('touchstart', onTouchStart)
  el.removeEventListener('touchmove', onTouchMove)
  el.removeEventListener('touchend', onTouchEnd)
  el.removeEventListener('touchcancel', onTouchEnd)
}

onMounted(() => {
  bindPullListeners()
  window.addEventListener('scroll', onWindowScrollLoadMore, { passive: true })
})

onBeforeUnmount(() => {
  disconnectLoadMoreObserver()
  unbindPullListeners()
  window.removeEventListener('scroll', onWindowScrollLoadMore)
  if (scrollLoadRaf) {
    cancelAnimationFrame(scrollLoadRaf)
    scrollLoadRaf = 0
  }
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

    <FeedHeader title="名人榜" :loading="loading" :refreshing="refreshing" @refresh="load(true)" />

    <AlbumViewer
      v-if="previewVisible"
      :visible="previewVisible"
      :loading="previewLoading"
      :title="previewTitle"
      :description="previewDescription"
      :images="previewImages"
      :active-index="activeIndex"
      @back="closePreview"
    />

    <div v-else-if="error" class="error-wrap">
      <p class="error">{{ error }}</p>
      <button @click="load(true)">重试</button>
    </div>
    <section v-else-if="loading && !list.length" class="waterfall-container">
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
    <div v-else-if="!list.length && !loading" class="empty">暂无内容</div>

    <section v-else class="waterfall-container">
      <div v-if="refreshing" class="feed-refresh-bar" aria-live="polite">正在更新列表…</div>
      <VirtualWaterfall
        :items="waterfallItems"
        row-key="id"
        :item-min-width="WATERFALL_LAYOUT.itemMinWidth"
        :min-column-count="WATERFALL_LAYOUT.minColumnCount"
        :max-column-count="WATERFALL_LAYOUT.maxColumnCount"
        :gap="WATERFALL_LAYOUT.gap"
        :padding="WATERFALL_LAYOUT.padding"
        :calc-item-height="calcItemHeight"
      >
        <template #default="{ item }">
          <AlbumCard :item="item" @select="openPreview" />
        </template>
      </VirtualWaterfall>
      <div ref="sentinelRef" class="feed-scroll-sentinel" aria-hidden="true" />
    </section>

    <div v-if="!error && list.length > 0" class="feed-status">
      <template v-if="hasMore">
        <button
          type="button"
          class="feed-load-more-btn"
          :disabled="loadingMore || refreshing"
          @click="loadMore"
        >
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </template>
      <span v-else class="feed-status-end">已经到底啦</span>
    </div>
  </main>
</template>
