<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminLogin } from '../api/authApi'

const router = useRouter()
const route = useRoute()

const password = ref('')
const loading = ref(false)
const error = ref('')

const onSubmit = async () => {
  error.value = ''
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  loading.value = true
  try {
    const { data } = await adminLogin(password.value)
    const payload = data?.data ?? {}
    const token = payload.token
    if (data?.code !== 0 || !token) {
      error.value = data?.message || '登录失败'
      return
    }
    sessionStorage.setItem('adminToken', token)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect || '/')
  } catch (err) {
    error.value = err?.response?.data?.message ?? err?.message ?? '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <aside class="login-hero">
      <div class="login-brand">
        <div class="login-brand-mark">SA</div>
        <h1>欢迎回来</h1>
        <p>后台管理需验证后继续。设计风格参考 Sakai：清爽分区、松石绿主色与轻拟物卡片。</p>
      </div>
    </aside>
    <section class="login-aside">
      <div class="login-card">
        <h2>登录</h2>
        <p class="login-sub">请输入管理后台密码</p>
        <form @submit.prevent="onSubmit">
          <div class="login-field">
            <label for="admin-password">密码</label>
            <input
              id="admin-password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••"
              :disabled="loading"
            />
          </div>
          <p v-if="error" class="login-error">{{ error }}</p>
          <button type="submit" class="login-submit" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>
        </form>
        <p class="login-footnote">默认密码见后端 app.admin.password，可用环境变量 ADMIN_PASSWORD 覆盖</p>
      </div>
    </section>
  </div>
</template>
