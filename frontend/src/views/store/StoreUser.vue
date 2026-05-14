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
      <el-form-item label="门店">
        <el-select v-model="searchForm.storeId" placeholder="全部" clearable style="width:200px">
          <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="searchForm.username" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="storeName" label="门店" min-width="150" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="用户类型" width="100">
          <template #default="{ row }">{{ row.userType === 2 ? '店长' : row.userType === 1 ? '总部' : '投资人' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import PageLayout from '@/components/PageLayout.vue'
import { getList as getStoreOptions } from '@/api/store'
import { getList } from '@/api/user'

const loading = ref(false)
const searchForm = reactive({ storeId: null, username: '' })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const storeOptions = ref([])

async function loadStoreOptions() {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await getList({ ...searchForm, page: page.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() {
  searchForm.storeId = null; searchForm.username = ''
  page.value = 1; loadData()
}
function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }

onMounted(() => {
  loadStoreOptions()
  loadData()
})
</script>
