<template>
  <div class="stat-card" :style="{ borderTopColor: color }">
    <div class="stat-card-content">
      <div class="stat-card-info">
        <div class="stat-card-title">{{ title }}</div>
        <div class="stat-card-value">{{ value ?? '-' }}</div>
      </div>
      <div class="stat-card-icon" v-if="icon">
        <el-icon :size="36" :color="color">
          <component :is="icon" />
        </el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], default: '-' },
  icon: { type: String, default: '' },
  color: { type: String, default: '#1890ff' }
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  border-top: 3px solid #1890ff;
  transition: box-shadow 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card-info {
  flex: 1;
}

.stat-card-title {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.stat-card-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-card-icon {
  margin-left: 16px;
  flex-shrink: 0;
}
</style>
