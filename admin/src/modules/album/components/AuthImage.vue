<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  alt: {
    type: String,
    default: ''
  }
})

const displaySrc = ref('')
let objectUrl = ''

function cleanupObjectUrl() {
  if (!objectUrl) return
  URL.revokeObjectURL(objectUrl)
  objectUrl = ''
}

async function loadImage(target) {
  cleanupObjectUrl()
  if (!target) {
    displaySrc.value = ''
    return
  }
  const token = sessionStorage.getItem('adminToken')
  const headers = {}
  if (token) {
    headers['X-Admin-Token'] = token
    headers.Authorization = `Bearer ${token}`
  }
  try {
    const res = await fetch(target, { headers })
    if (!res.ok) {
      throw new Error(`image request failed: ${res.status}`)
    }
    const blob = await res.blob()
    objectUrl = URL.createObjectURL(blob)
    displaySrc.value = objectUrl
  } catch {
    // Fallback keeps image visible when endpoint is public.
    displaySrc.value = target
  }
}

watch(
  () => props.src,
  (value) => {
    loadImage(value)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  cleanupObjectUrl()
})
</script>

<template>
  <img v-bind="$attrs" :src="displaySrc" :alt="alt" />
</template>
