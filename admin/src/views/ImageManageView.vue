<script setup>
import AdminLayout from '../modules/dashboard/components/AdminLayout.vue'
import StatsCards from '../modules/dashboard/components/StatsCards.vue'
import TopAlbumsPanel from '../modules/analytics/components/TopAlbumsPanel.vue'
import TodayDevicePiePanel from '../modules/analytics/components/TodayDevicePiePanel.vue'
import { useAnalyticsDashboard } from '../modules/analytics/composables/useAnalyticsDashboard'

const {
  loading,
  error,
  stats,
  topAlbums,
  todayDevicePv,
  events,
  page,
  total,
  totalPages,
  hasPrevPage,
  hasNextPage,
  load,
  clearEvents,
  prevPage,
  nextPage
} = useAnalyticsDashboard()
</script>

<template>
  <AdminLayout :loading="loading" @refresh="load">
    <StatsCards :stats="stats" />
    <section class="content-grid">
      <TopAlbumsPanel :items="topAlbums" />
      <TodayDevicePiePanel :items="todayDevicePv" />
      <article class="panel full">
        <div class="panel-header">
          <h2>埋点记录</h2>
          <button @click="clearEvents">清空记录</button>
        </div>
        <div class="event-table-body">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>相册</th>
                <th>访客ID</th>
                <th>IP</th>
                <th>设备</th>
                <th>version</th>
                <th>访问时间(Unix)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="event in events" :key="event.id">
                <td>{{ event.id }}</td>
                <td>{{ event.albumTitle }}</td>
                <td>{{ event.visitorId }}</td>
                <td>{{ event.ip }}</td>
                <td>{{ event.device }}</td>
                <td>{{ event.deviceVersion }}</td>
                <td>{{ event.viewedAt }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination-bar">
          <p>共 {{ total }} 条 · 第 {{ page }} / {{ totalPages }} 页</p>
          <div class="ops">
            <button :disabled="!hasPrevPage" @click="prevPage">上一页</button>
            <button :disabled="!hasNextPage" @click="nextPage">下一页</button>
          </div>
        </div>
      </article>
    </section>
  </AdminLayout>
</template>

<style scoped>
.event-table-body {
  height: 540px;
  overflow: auto;
}
</style>
