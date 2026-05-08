<script setup>
import { VirtualWaterfall } from '@lhlyu/vue-virtual-waterfall'
import { ref } from 'vue'
import FeedHeader from '../modules/feed/components/FeedHeader.vue'
import AlbumViewer from '../modules/feed/components/AlbumViewer.vue'
import AlbumCard from '../modules/feed/components/AlbumCard.vue'
import { WATERFALL_LAYOUT } from '../modules/feed/constants/waterfall'
import { useFeedAlbums } from '../modules/feed/composables/useFeedAlbums'

const waterfallRef = ref(null)

const {
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
} = useFeedAlbums()

const handleWaterfallScroll = () => {
  const el = waterfallRef.value
  if (!el || loading.value || loadingMore.value || !hasMore.value) return
  const threshold = 180
  const nearBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - threshold
  if (nearBottom) {
    loadMore()
  }
}
</script>

<template>
  <main class="feed-page">
    <FeedHeader title="名人榜" :loading="loading" @refresh="load(true)" />

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
    <section v-else-if="loading" class="waterfall-container">
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
    <div v-else-if="list.length === 0" class="empty">暂无内容</div>

    <section v-else ref="waterfallRef" class="waterfall-container" @scroll.passive="handleWaterfallScroll">
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
    </section>

    <div v-if="!error && list.length > 0" class="feed-status">
      <span v-if="loadingMore">正在加载更多...</span>
      <span v-else-if="!hasMore">已经到底啦</span>
    </div>
  </main>
</template>
