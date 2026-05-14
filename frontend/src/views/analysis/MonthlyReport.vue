<template>
  <div class="monthly-report" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="门店">
          <el-select v-model="storeId" placeholder="全部" clearable @change="loadData" style="width:200px">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-date-picker
            v-model="month"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            @change="loadData"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 月汇总卡片 -->
    <el-row :gutter="16" class="stat-row" v-if="monthlyData">
      <el-col :xs="24" :sm="8" :lg="4">
        <StatCard title="本月总营收" :value="'¥' + formatMoney(monthlyData.totalRevenue)" icon="Money" color="#1890ff" />
      </el-col>
      <el-col :xs="24" :sm="8" :lg="4">
        <StatCard title="本月间夜数" :value="monthlyData.totalRooms" icon="House" color="#52c41a" />
      </el-col>
      <el-col :xs="24" :sm="8" :lg="4">
        <StatCard title="平均出租率" :value="monthlyData.avgOccupancy + '%'" icon="TrendCharts" color="#fa8c16" />
      </el-col>
      <el-col :xs="24" :sm="8" :lg="4">
        <StatCard title="平均ADR" :value="'¥' + monthlyData.avgADR" icon="PieChart" color="#722ed1" />
      </el-col>
      <el-col :xs="24" :sm="8" :lg="4">
        <StatCard title="平均RevPAR" :value="'¥' + monthlyData.avgRevPAR" icon="Histogram" color="#eb2f96" />
      </el-col>
    </el-row>

    <!-- 渠道间夜 -->
    <el-card shadow="never">
      <template #header>渠道间夜统计</template>
      <el-table :data="channelData" border stripe style="width:100%">
        <el-table-column prop="channelName" label="渠道" min-width="120" />
        <el-table-column prop="rooms" label="间夜数" align="right" />
        <el-table-column label="占比" align="right">
          <template #default="{ row }">{{ row.ratio }}%</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import StatCard from '@/components/StatCard.vue'
import { getMonthly } from '@/api/analysis'
import { getStoreOptions } from '@/api/store'

const loading = ref(false)
const storeOptions = ref([])
const storeId = ref(null)
const month = ref(new Date().toISOString().slice(0, 7))
const monthlyData = ref(null)
const channelData = ref([])

function formatMoney(val) {
  if (val == null) return '0'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMonthly({ storeId: storeId.value, month: month.value })
    monthlyData.value = res.data?.summary || null
    channelData.value = res.data?.channels || []
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
.monthly-report {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row .el-col {
  margin-bottom: 16px;
}
</style>
