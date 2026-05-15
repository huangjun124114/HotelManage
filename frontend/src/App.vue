<template>
  <div id="app-root">
    <router-view v-if="ready" />
    <div v-else class="app-loading">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured, onMounted } from 'vue'

const ready = ref(false)

onErrorCaptured((err, instance, info) => {
  console.error('App error:', err, info)
  return false
})

onMounted(() => {
  // 清除可能的坏数据
  try {
    const token = localStorage.getItem('token')
    if (token && token.length < 10) {
      localStorage.clear()
    }
  } catch(e) {}
  ready.value = true
})
</script>

<style>
#app-root {
  width: 100%;
  height: 100vh;
}
.app-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  font-size: 18px;
  color: #666;
}
</style>
