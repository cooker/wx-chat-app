<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Pagination } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/pagination'
import LazyImage from '../components/LazyImage.vue'
import { fetchAlbumDetail, reportAlbumView, toAssetUrl } from '../api/images'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const album = ref(null)
const swiperModules = [Pagination]

const albumId = computed(() => Number(route.params.id))
const images = computed(() => album.value?.images || [])

function getVisitorId() {
  const key = 'wx-chat-visitor-id'
  const current = window.localStorage.getItem(key)
  if (current) return current
  const created = `visitor-${crypto.randomUUID()}`
  window.localStorage.setItem(key, created)
  return created
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const id = albumId.value
    if (!Number.isFinite(id) || id <= 0) {
      throw new Error('相册ID无效')
    }
    const { data } = await fetchAlbumDetail(id)
    album.value = data?.data || null
    reportAlbumView(id, getVisitorId()).catch(() => {})
  } catch (err) {
    error.value = err?.message || '加载相册详情失败'
  } finally {
    loading.value = false
  }
}

function backToFeed() {
  router.push({ name: 'feed' })
}

onMounted(loadDetail)
</script>

<template>
  <main class="album-detail-page">
    <header class="album-detail-header">
      <button type="button" class="back-btn" @click="backToFeed">返回</button>
      <h1 class="title">相册详情</h1>
      <span class="placeholder" />
    </header>

    <section v-if="loading" class="state-text">加载中...</section>
    <section v-else-if="error" class="state-text state-error">
      <p>{{ error }}</p>
      <button type="button" class="retry-btn" @click="loadDetail">重试</button>
    </section>
    <template v-else>
      <section class="meta">
        <h2>{{ album?.title || '未命名相册' }}</h2>
        <p>{{ album?.description || '暂无描述' }}</p>
      </section>
      <swiper
        v-if="images.length > 0"
        class="album-swiper"
        :modules="swiperModules"
        :slides-per-view="1"
        :space-between="12"
        :pagination="{ clickable: true }"
      >
        <swiper-slide v-for="(image, idx) in images" :key="image.id || `${image.url}-${idx}`">
          <article class="image-card">
            <LazyImage
              layout="block"
              :src="toAssetUrl(image.url)"
              :alt="image.originalName || `图片${idx + 1}`"
            />
            <p>{{ image.originalName || `图片${idx + 1}` }}</p>
          </article>
        </swiper-slide>
      </swiper>
      <section v-else class="state-text">这个相册还没有图片</section>
    </template>
  </main>
</template>

<style scoped>
.album-detail-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 12px;
  min-height: 100vh;
  background: #f7f8fa;
}

.album-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.title {
  margin: 0;
  font-size: 18px;
}

.placeholder {
  width: 42px;
}

.back-btn,
.retry-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 10px;
  padding: 8px 12px;
  color: #334155;
}

.meta {
  margin-bottom: 14px;
}

.meta h2 {
  margin: 0;
  font-size: 22px;
}

.meta p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.5;
}

.album-swiper {
  width: 100%;
  padding-bottom: 26px;
}

.image-card {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.image-card :deep(.lazy-img) {
  border-radius: 12px 12px 0 0;
}

.image-card p {
  margin: 0;
  padding: 8px 10px 10px;
  font-size: 13px;
  color: #6b7280;
}

.album-swiper :deep(.swiper-pagination-bullet) {
  background: #94a3b8;
  opacity: 0.7;
}

.album-swiper :deep(.swiper-pagination-bullet-active) {
  background: #2563eb;
  opacity: 1;
}

.state-text {
  color: #6b7280;
  text-align: center;
  margin-top: 40px;
}

.state-error {
  color: #b91c1c;
}
</style>
