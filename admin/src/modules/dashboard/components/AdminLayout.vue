<script setup>
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'
import { sidebarMenus } from '../constants/navigation'

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh'])
</script>

<template>
  <div class="layout">
    <AppSidebar :menus="sidebarMenus" />
    <section class="main">
      <AppTopbar :loading="loading" @refresh="emit('refresh')" />
      <slot />
    </section>
  </div>
</template>
