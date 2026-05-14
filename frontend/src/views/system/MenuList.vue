<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>菜单管理</span>
        <el-button type="primary" size="small" @click="handleAdd(null)"><el-icon><Plus /></el-icon> 新增</el-button>
      </div>
    </template>
    <el-table :data="menuTree" row-key="id" border stripe v-loading="loading" default-expand-all>
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="path" label="路径" width="180" />
      <el-table-column prop="icon" label="图标" width="100" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ row.type === 1 ? '目录' : row.type === 2 ? '菜单' : '按钮' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getTree } from '@/api/menu'

const loading = ref(false)
const menuTree = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await getTree()
    menuTree.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleAdd(parent) {
  // 预留新增菜单功能
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
