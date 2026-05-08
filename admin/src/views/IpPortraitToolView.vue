<script setup>
import { computed, ref } from 'vue'
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import { useIpPortraitBrief } from '../modules/tools/ipPortrait/composables/useIpPortraitBrief'

const ipInput = ref('')
const { loading, error, result, query, clear } = useIpPortraitBrief()

const formattedJson = computed(() => {
  if (!result.value) return ''
  try {
    return JSON.stringify(result.value, null, 2)
  } catch {
    return String(result.value)
  }
})

const onQuery = async () => {
  clear()
  await query(ipInput.value.trim())
}

const onRefresh = () => {
  if (ipInput.value.trim()) {
    onQuery()
  }
}
</script>

<template>
  <AdminLayout :loading="loading" @refresh="onRefresh">
    <article class="panel full ip-tool-panel">
      <h2>IP 画像查询（奇符 brief-info）</h2>
      <p class="hint">
        通过本机后端代理请求上游接口，并自动附带 Origin / Referer。请填写 IPv4 或 IPv6。
      </p>
      <div class="ip-tool-form">
        <label>
          IP 地址
          <input v-model="ipInput" type="text" placeholder="例如：39.185.155.129" @keyup.enter="onQuery" />
        </label>
        <button type="button" :disabled="loading" @click="onQuery">查询</button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <pre v-if="formattedJson" class="ip-tool-json">{{ formattedJson }}</pre>
    </article>
  </AdminLayout>
</template>

<style scoped>
.ip-tool-panel h2 {
  margin-top: 0;
}
.hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
}
.ip-tool-form {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-bottom: 12px;
}
.ip-tool-form label {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}
.ip-tool-form input {
  border: 1px solid #d4dde8;
  border-radius: 8px;
  padding: 8px;
  font: inherit;
}
.ip-tool-json {
  margin: 0;
  padding: 12px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.5;
  overflow: auto;
  max-height: min(70vh, 640px);
}
.error {
  color: #b91c1c;
  font-size: 13px;
  margin: 0 0 8px;
}
</style>
