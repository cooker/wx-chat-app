<script setup>
import LazyImage from '../../../components/LazyImage.vue'
import { CARD_COVER_RATIO_DEFAULT } from '../constants/waterfall'

defineProps({
  item: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['select'])
</script>

<template>
  <article class="card" @click="emit('select', item.id)">
    <LazyImage
      v-if="item.coverDisplay"
      layout="cover"
      :cover-ratio="item.ratio || CARD_COVER_RATIO_DEFAULT"
      :src="item.coverDisplay"
      :alt="item.title"
    />
    <div class="card-body">
      <h3 class="card-title">{{ item.title }}</h3>
      <div class="card-meta">
        <span class="card-author">{{ item.authorText }}</span>
        <span class="card-like">♡ {{ item.likeText }}</span>
      </div>
    </div>
  </article>
</template>
