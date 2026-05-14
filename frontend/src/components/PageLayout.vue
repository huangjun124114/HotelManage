<template>
  <div class="page-layout">
    <!-- 搜索区域 -->
    <el-card v-if="showSearch" class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <slot name="search" />
        <el-form-item>
          <el-button type="primary" @click="$emit('search')">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="$emit('reset')">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 -->
    <div class="toolbar" v-if="$slots.toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 表格区域 -->
    <el-card shadow="never">
      <slot name="table" />
    </el-card>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="$emit('size-change', $event)"
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  searchForm: { type: Object, default: () => ({}) },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 20 },
  showSearch: { type: Boolean, default: true }
})

defineEmits(['search', 'reset', 'size-change', 'page-change'])

const currentPage = computed({
  get: () => props.page,
  set: (val) => {}
})

const pageSize = computed({
  get: () => props.size,
  set: (val) => {}
})
</script>

<style scoped>
.page-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  margin-bottom: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}
</style>
