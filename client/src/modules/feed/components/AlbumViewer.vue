<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import { Swiper, SwiperSlide } from 'swiper/vue'
import 'swiper/css'
import { toAssetUrl } from '../../../api/images'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  images: {
    type: Array,
    default: () => []
  },
  activeIndex: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['back'])

const markdownDescription = computed(() =>
  marked.parse(props.description || '', {
    breaks: true
  })
)
</script>

<template>
  <section v-if="visible" class="album-viewer">
    <header class="viewer-header">
      <button class="back-btn" @click="emit('back')">返回</button>
      <div class="viewer-title">
        <h3>{{ title }}</h3>
      </div>
    </header>
    <p v-if="loading" class="preview-loading">正在加载相册图片...</p>
    <Swiper :initial-slide="activeIndex" class="viewer-swiper">
      <SwiperSlide v-for="(item, idx) in images" :key="idx">
        <div class="slide-content">
          <img class="preview-image" :src="toAssetUrl(item.url)" :alt="item.originalName || title" />
        </div>
      </SwiperSlide>
    </Swiper>
    <article v-if="description" class="viewer-description markdown-body" v-html="markdownDescription"></article>
  </section>
</template>
