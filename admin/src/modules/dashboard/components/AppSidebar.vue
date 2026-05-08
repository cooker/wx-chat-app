<script setup>
import { useRoute } from 'vue-router'

defineProps({
  menus: {
    type: Array,
    required: true
  }
})

const route = useRoute()
</script>

<template>
  <aside class="sidebar">
    <div class="brand">SAKAI</div>
    <nav class="menu">
      <RouterLink
        v-for="menu in menus"
        :key="menu.to"
        :to="menu.to"
        :class="{ active: route.path === menu.to }"
      >
        {{ menu.label }}
      </RouterLink>
    </nav>
  </aside>
</template>
