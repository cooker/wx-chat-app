<script setup>
import { computed, ref } from 'vue'
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import { useIpPortraitBrief } from '../modules/tools/ipPortrait/composables/useIpPortraitBrief'
import { useTodayIpPortraits } from '../modules/tools/ipPortrait/composables/useTodayIpPortraits'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const ipInput = ref('')
const { loading, error, result, query, clear } = useIpPortraitBrief()

const {
  loading: todayLoading,
  error: todayError,
  rows: todayRows,
  fetchDone: todayFetchDone,
  load: loadTodayIpPortraits
} = useTodayIpPortraits()

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

const onLoadToday = () => {
  loadTodayIpPortraits()
}
</script>

<template>
  <AdminLayout :loading="loading || todayLoading" @refresh="onRefresh">
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

      <section class="today-ip-section">
        <div class="today-ip-header">
          <h3>今日访问 IP（去重）</h3>
          <Button
            label="加载并解析城市"
            icon="pi pi-sync"
            size="small"
            :loading="todayLoading"
            :disabled="todayLoading"
            @click="onLoadToday"
          />
        </div>
        <p class="hint">
          数据来自今日埋点 <code>access_events</code>（与统计看板同一时区自然日）；逐条调用
          <code>fetchIpPortraitBrief</code>（<code>/api/tools/ip-portrait</code>）解析城市。
        </p>
        <p v-if="todayError" class="error">{{ todayError }}</p>
        <DataTable
          v-if="todayLoading || todayRows.length > 0 || todayFetchDone"
          :value="todayRows"
          size="small"
          striped-rows
          :empty-message="todayLoading ? '正在拉取画像…' : '今日暂无去重 IP 记录'"
        >
          <Column field="ip" header="IP" />
          <Column field="city" header="城市">
            <template #body="{ data }">
              <span>{{ data.city || '—' }}</span>
            </template>
          </Column>
          <Column field="error" header="备注">
            <template #body="{ data }">
              <span v-if="data.error" class="error">{{ data.error }}</span>
              <span v-else class="muted">—</span>
            </template>
          </Column>
        </DataTable>
        <p v-else class="muted">点击「加载并解析城市」查看今日去重 IP。</p>
      </section>
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
.today-ip-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}
.today-ip-section h3 {
  margin: 0;
  font-size: 1.05rem;
}
.today-ip-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.muted {
  color: #94a3b8;
  font-size: 13px;
}
.today-ip-section code {
  font-size: 12px;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
