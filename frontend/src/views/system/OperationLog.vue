<template>
  <PageLayout
    :search-form="searchForm"
    :total="total"
    :page="page"
    :size="size"
    @search="handleSearch"
    @reset="handleReset"
    @size-change="handleSizeChange"
    @page-change="handlePageChange"
  >
    <template #search>
      <el-form-item label="操作人">
        <el-input v-model="searchForm.operator" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="操作模块">
        <el-input v-model="searchForm.module" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width:280px"
        />
      </el-form-item>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="action" label="操作类型" width="100" />
        <el-table-column prop="description" label="操作描述" min-width="200" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="170" />
      </el-table>
    </template>
  </PageLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import PageLayout from '@/components/PageLayout.vue'
import request from '@/utils/request'

const loading = ref(false)
const searchForm = reactive({ operator: '', module: '', dateRange: null })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, ...searchForm }
    if (params.dateRange) {
      params.startDate = params.dateRange[0]
      params.endDate = params.dateRange[1]
      delete params.dateRange
    } else {
      delete params.startDate; delete params.endDate
    }
    const res = await request({ url: '/operation-logs', method: 'get', params })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() {
  searchForm.operator = ''; searchForm.module = ''; searchForm.dateRange = null
  page.value = 1; loadData()
}
function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }

onMounted(() => loadData())
</script>
