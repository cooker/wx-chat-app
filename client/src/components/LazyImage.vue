<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

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
  },
  /** cover 布局的高宽比（高/宽），与瀑布流 calcItemHeight 保持一致 */
  coverRatio: {
    type: Number,
    default: 1.18
  }
})

const coverAspectStyle = computed(() =>
  props.layout === 'cover' ? { aspectRatio: `1 / ${props.coverRatio}` } : undefined
)

const loaded = ref(false)
const failed = ref(false)
const imgRef = ref(null)
const rootRef = ref(null)
let observer = null
let resizeObserver = null
let scrollHandler = null
let revealGen = 0

/** 虚拟瀑布流 + 横滑区：避免原生 lazy 与节点回收冲突 */
const imgLoadingAttr = computed(() =>
  props.layout === 'cover' || props.layout === 'fill' ? 'eager' : 'lazy'
)

const imgFetchPriority = computed(() =>
  props.layout === 'cover' || props.layout === 'fill' ? 'auto' : 'low'
)

function resetState() {
  loaded.value = false
  failed.value = false
  revealGen += 1
}

function isInViewport() {
  const root = rootRef.value
  if (!root) return false
  const rect = root.getBoundingClientRect()
  if (rect.width <= 0 || rect.height <= 0) return false
  const vh = window.innerHeight || document.documentElement.clientHeight || 0
  const vw = window.innerWidth || document.documentElement.clientWidth || 0
  return rect.bottom > 0 && rect.top < vh && rect.right > 0 && rect.left < vw
}

function markLoadedIfReady(gen, expectedSrc, el) {
  if (gen !== revealGen || props.src !== expectedSrc) return false
  const current = imgRef.value
  if (!current || current !== el) return false
  if (current.complete && current.naturalWidth > 0) {
    loaded.value = true
    teardownScrollRetry()
    return true
  }
  return false
}

async function tryReveal() {
  const gen = ++revealGen
  const expectedSrc = props.src
  await nextTick()
  if (gen !== revealGen || props.src !== expectedSrc) return

  const el = imgRef.value
  if (!el || !expectedSrc || failed.value) return

  if (markLoadedIfReady(gen, expectedSrc, el)) return

  if (typeof el.decode === 'function') {
    try {
      await el.decode()
    } catch {
      // 解码失败仍等 load / error
    }
  }
  if (gen !== revealGen || props.src !== expectedSrc) return
  markLoadedIfReady(gen, expectedSrc, el)
}

function scheduleTryReveal() {
  requestAnimationFrame(() => {
    void tryReveal()
  })
}

function teardownScrollRetry() {
  if (!scrollHandler) return
  window.removeEventListener('scroll', scrollHandler, { capture: true })
  scrollHandler = null
}

function bindScrollRetry() {
  if (scrollHandler || loaded.value) return
  scrollHandler = () => {
    if (loaded.value) {
      teardownScrollRetry()
      return
    }
    if (isInViewport()) scheduleTryReveal()
  }
  window.addEventListener('scroll', scrollHandler, { passive: true, capture: true })
}

function bindResizeObserver() {
  if (typeof ResizeObserver === 'undefined') return
  resizeObserver?.disconnect()
  const root = rootRef.value
  if (!root) return
  resizeObserver = new ResizeObserver(() => {
    if (!loaded.value) scheduleTryReveal()
  })
  resizeObserver.observe(root)
}

watch(
  () => props.src,
  () => {
    resetState()
    bindScrollRetry()
    scheduleTryReveal()
  }
)

watch(imgRef, (el) => {
  if (el && props.src) scheduleTryReveal()
})

watch(loaded, (value) => {
  if (value) teardownScrollRetry()
})

function onLoad() {
  loaded.value = true
  teardownScrollRetry()
}

function onError() {
  failed.value = true
  loaded.value = true
  teardownScrollRetry()
}

function bindIntersection() {
  if (typeof IntersectionObserver === 'undefined') return
  observer?.disconnect()
  const root = rootRef.value
  if (!root) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) {
        scheduleTryReveal()
      }
    },
    { root: null, rootMargin: '320px 0px 480px 0px', threshold: 0 }
  )
  observer.observe(root)
}

function setupObservers() {
  bindIntersection()
  bindResizeObserver()
  bindScrollRetry()
  scheduleTryReveal()
}

onMounted(() => {
  scheduleTryReveal()
  nextTick(setupObservers)
})

onBeforeUnmount(() => {
  revealGen += 1
  observer?.disconnect()
  observer = null
  resizeObserver?.disconnect()
  resizeObserver = null
  teardownScrollRetry()
})
</script>

<template>
  <div
    ref="rootRef"
    class="lazy-img"
    :class="[
      `lazy-img--${layout}`,
      { 'lazy-img--is-loaded': loaded, 'lazy-img--is-failed': failed }
    ]"
    :style="coverAspectStyle"
  >
    <div v-show="!loaded" class="lazy-img__placeholder" aria-hidden="true" />
    <img
      v-if="src"
      :key="src"
      ref="imgRef"
      class="lazy-img__img"
      :class="imgClass"
      :src="src"
      :alt="alt"
      :loading="imgLoadingAttr"
      decoding="async"
      :fetchpriority="imgFetchPriority"
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
  /* 父级 VirtualWaterfall 使用 content-visibility:auto，子级需可见才能稳定解码图片 */
  content-visibility: visible;
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
  display: none;
}

.lazy-img--is-loaded .lazy-img__img {
  opacity: 1;
}

.lazy-img__img {
  display: block;
  position: relative;
  z-index: 2;
  opacity: 0;
  transition: opacity 0.22s ease;
}

.lazy-img--cover {
  width: 100%;
  flex-shrink: 0;
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
