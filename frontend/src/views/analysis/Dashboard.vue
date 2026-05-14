<template>
  <div class="dashboard" v-loading="loading">
    <!-- 日期选择 -->
    <el-card shadow="never" class="date-card">
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        value-format="YYYY-MM-DD"
        @change="loadData"
      />
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="应填报" :value="data.shouldFill" icon="Shop" color="#1890ff" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="已填报" :value="data.filled" icon="Checked" color="#52c41a" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="未填报" :value="data.unfilled" icon="Warning" color="#ff4d4f" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="总营收" :value="formatMoney(data.totalRevenue)" icon="Money" color="#722ed1" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="总间夜" :value="data.totalRooms" icon="House" color="#fa8c16" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="均出租率" :value="data.avgOccupancy + '%'" icon="TrendCharts" color="#13c2c2" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="均ADR" :value="'¥' + data.avgADR" icon="Coin" color="#eb2f96" />
      </el-col>
      <el-col :xs="24" :sm="12" :lg="3">
        <StatCard title="均RevPAR" :value="'¥' + data.avgRevPAR" icon="Histogram" color="#f5222d" />
      </el-col>
    </el-row>

    <!-- 图表行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>最近7日营收趋势</template>
          <div ref="revenueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>最近7日出租率趋势</template>
          <div ref="occupancyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 排名和未填报 -->
    <el-row :gutter="16" class="table-row">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>门店营收排名</template>
          <el-table :data="data.revenueRanking || []" size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="storeName" label="门店" />
            <el-table-column label="营收">
              <template #default="{ row }">¥{{ formatMoney(row.revenue) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>门店出租率排名</template>
          <el-table :data="data.occupancyRanking || []" size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="storeName" label="门店" />
            <el-table-column label="出租率">
              <template #default="{ row }">{{ row.occupancy }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 未填报门店 -->
    <el-card shadow="never" v-if="data.unfilledStores?.length">
      <template #header>未填报门店</template>
      <el-table :data="data.unfilledStores" size="small">
        <el-table-column prop="storeName" label="门店" />
        <el-table-column prop="managerName" label="店长" />
        <el-table-column prop="phone" label="电话" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import StatCard from '@/components/StatCard.vue'
import { getDashboard } from '@/api/analysis'
import * as echarts from 'echarts'

const loading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const revenueChartRef = ref(null)
const occupancyChartRef = ref(null)

const data = reactive({
  shouldFill: 0, filled: 0, unfilled: 0,
  totalRevenue: 0, totalRooms: 0, avgOccupancy: 0,
  avgADR: '', avgRevPAR: '',
  dateLabels: [], revenueTrend: [], occupancyTrend: [],
  revenueRanking: [], occupancyRanking: [], unfilledStores: []
})

function formatMoney(val) {
  if (val == null || val === '') return '¥0'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function initCharts() {
  if (revenueChartRef.value) {
    const chart = echarts.init(revenueChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: data.dateLabels, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [{
        name: '营收', type: 'line', data: data.revenueTrend, smooth: true,
        itemStyle: { color: '#1890ff' }
      }]
    })
  }

  if (occupancyChartRef.value) {
    const chart = echarts.init(occupancyChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: data.dateLabels, boundaryGap: false },
      yAxis: { type: 'value', max: 100 },
      series: [{
        name: '出租率(%)', type: 'line', data: data.occupancyTrend, smooth: true,
        itemStyle: { color: '#52c41a' }
      }]
    })
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDashboard({ date: selectedDate.value })
    const d = res.data || {}
    data.shouldFill = d.shouldFill ?? 0
    data.filled = d.filled ?? 0
    data.unfilled = d.unfilled ?? 0
    data.totalRevenue = d.totalRevenue ?? 0
    data.totalRooms = d.totalRooms ?? 0
    data.avgOccupancy = d.avgOccupancy ?? 0
    data.avgADR = d.avgADR ?? ''
    data.avgRevPAR = d.avgRevPAR ?? ''
    data.dateLabels = d.dateLabels || []
    data.revenueTrend = d.revenueTrend || []
    data.occupancyTrend = d.occupancyTrend || []
    data.revenueRanking = d.revenueRanking || []
    data.occupancyRanking = d.occupancyRanking || []
    data.unfilledStores = d.unfilledStores || []
    nextTick(() => initCharts())
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row .el-col { margin-bottom: 16px; }

.chart-container { height: 300px; }
</style>
