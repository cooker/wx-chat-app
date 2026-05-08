<script setup>
import { useRouter } from 'vue-router'
import { adminLogout } from '../../../api/authApi'

defineProps({
  loading: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['refresh'])
const router = useRouter()

const logout = async () => {
  try {
    await adminLogout()
  } catch {
    /* ignore */
  }
  sessionStorage.removeItem('adminToken')
  router.push('/login')
}
</script>

<template>
  <header class="topbar">
    <h1>Dashboard</h1>
    <div class="top-actions">
      <button @click="emit('refresh')" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
      <button type="button" class="btn-logout" @click="logout">退出</button>
      <span class="avatar">G</span>
    </div>
  </header>
</template>
