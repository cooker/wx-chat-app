<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  },
  error: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['edit', 'remove', 'toggle'])
</script>

<template>
  <article class="panel">
    <h2>图片管理</h2>
    <p v-if="error" class="error">{{ error }}</p>
    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>标题</th>
          <th>作者</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.title }}</td>
          <td>{{ item.authorName }}</td>
          <td>{{ item.online ? '已上架' : '未上架' }}</td>
          <td class="ops">
            <button @click="emit('edit', item)">编辑</button>
            <button @click="emit('remove', item)">删除</button>
            <button @click="emit('toggle', item)">
              {{ item.online ? '下架' : '上架' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </article>
</template>
