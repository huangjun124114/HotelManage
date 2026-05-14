<template>
  <div class="daily-report" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="门店">
          <el-select v-model="searchForm.storeId" placeholder="全部" clearable @change="loadData" style="width:200px">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="searchForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            @change="loadData"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>日报数据</template>
      <el-table :data="tableData" border stripe style="width:100%">
        <el-table-column prop="storeName" label="门店" min-width="130" />
        <el-table-column prop="reportDate" label="日期" width="110" />
        <el-table-column prop="rooms" label="间夜数" width="80" align="right" />
        <el-table-column label="出租率" width="80" align="right">
          <template #default="{ row }">{{ row.occupancy }}%</template>
        </el-table-column>
        <el-table-column label="ADR" width="100" align="right">
          <template #default="{ row }">¥{{ row.adr?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="总营收" width="120" align="right">
          <template #default="{ row }">¥{{ row.totalRevenue?.toLocaleString() }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getStoreOptions } from '@/api/store'
import { queryList } from '@/api/report'

const loading = ref(false)
const storeOptions = ref([])
const tableData = ref([])
const searchForm = reactive({ storeId: null, date: new Date().toISOString().slice(0, 10) })

async function loadData() {
  loading.value = true
  try {
    const params = { date: searchForm.date }
    if (searchForm.storeId) params.storeId = searchForm.storeId
    const res = await queryList(params)
    tableData.value = res.data?.records || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

onMounted(async () => {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []
  loadData()
})
</script>

<style scoped>
.daily-report {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
