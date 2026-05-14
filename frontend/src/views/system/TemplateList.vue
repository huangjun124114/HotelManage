<template>
  <PageLayout :show-search="false" :total="total" :page="page" :size="size"
    @size-change="handleSizeChange" @page-change="handlePageChange">
    <template #toolbar>
      <span></span>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增模板</el-button>
    </template>
    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column prop="code" label="模板编码" width="150" />
        <el-table-column label="是否默认" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" size="small">
              {{ row.isDefault === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import request from '@/utils/request'

const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await request({ url: '/templates', method: 'get', params: { page: page.value, size: size.value } })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }
function handleAdd() {}
function handleEdit(row) {}

onMounted(() => loadData())
</script>
