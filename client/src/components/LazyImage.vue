<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  alt: {
    type: String,
    default: ''
  },
  /** 追加在内部 img 上的 class */
  imgClass: {
    type: String,
    default: ''
  },
  /**
   * cover：固定比例铺满（瀑布流卡片）
   * fill：铺满父容器（热门横滑按钮内）
   * contain：限高 contain（大图预览）
   * block：宽 100% 高度随图（详情轮播）
   */
  layout: {
    type: String,
    default: 'cover',
    validator: (v) => ['cover', 'fill', 'contain', 'block'].includes(v)
  }
})

const loaded = ref(false)
const failed = ref(false)
const imgRef = ref(null)

function resetState() {
  loaded.value = false
  failed.value = false
}

watch(
  () => props.src,
  () => {
    resetState()
    syncCachedDecode()
  }
)

function syncCachedDecode() {
  nextTick(() => {
    const el = imgRef.value
    if (el?.complete && el.naturalWidth > 0) {
      loaded.value = true
    }
  })
}

onMounted(syncCachedDecode)

function onLoad() {
  loaded.value = true
}

function onError() {
  failed.value = true
  loaded.value = true
}
</script>

<template>
  <div
    class="lazy-img"
    :class="[
      `lazy-img--${layout}`,
      { 'lazy-img--is-loaded': loaded, 'lazy-img--is-failed': failed }
    ]"
  >
    <div v-show="!loaded" class="lazy-img__placeholder" aria-hidden="true" />
    <img
      v-if="src"
      ref="imgRef"
      class="lazy-img__img"
      :class="imgClass"
      :src="src"
      :alt="alt"
      loading="lazy"
      decoding="async"
      fetchpriority="low"
      @load="onLoad"
      @error="onError"
    />
  </div>
</template>

<style scoped>
.lazy-img {
  position: relative;
  overflow: hidden;
  background: #f3f4f6;
}

.lazy-img__placeholder {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(110deg, #eceff3 8%, #f8fafc 18%, #eceff3 33%);
  background-size: 200% 100%;
  animation: lazy-ph-shimmer 1.1s ease-in-out infinite;
}

.lazy-img--is-loaded .lazy-img__placeholder {
  opacity: 0;
  transition: opacity 0.28s ease;
}

.lazy-img--is-loaded .lazy-img__img {
  opacity: 1;
}

.lazy-img__img {
  display: block;
  opacity: 0;
  transition: opacity 0.28s ease;
}

.lazy-img--cover {
  width: 100%;
  aspect-ratio: 1 / 1.18;
}

.lazy-img--cover .lazy-img__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.lazy-img--fill {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.lazy-img--fill .lazy-img__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.lazy-img--contain {
  width: 100%;
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1f2937;
}

.lazy-img--contain .lazy-img__img {
  max-width: 100%;
  max-height: 76vh;
  width: auto;
  height: auto;
  object-fit: contain;
}

.lazy-img--block {
  width: 100%;
  min-height: 200px;
}

.lazy-img--block .lazy-img__img {
  width: 100%;
  height: auto;
}

.lazy-img--is-failed .lazy-img__img {
  opacity: 0.45;
  filter: grayscale(1);
}

@keyframes lazy-ph-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: 0 0;
  }
}
</style>
