<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <article class="panel full">
    <h2>Notifications</h2>
    <ul class="notifications">
      <li v-for="item in items" :key="item">{{ item }}</li>
    </ul>
  </article>
</template>
