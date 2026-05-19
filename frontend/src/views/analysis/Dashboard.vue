<template>
  <div class="dashboard" v-loading="loading">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8" :md="6">
          <span class="filter-label">基准日期</span>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="d => d > new Date()"
            @change="loadData"
            style="width: 100%"
          />
        </el-col>
        <el-col :xs="24" :sm="16" :md="12">
          <span class="filter-label">门店</span>
          <el-select
            v-model="selectedStores"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部门店"
            clearable
            @change="loadData"
            style="width: 100%"
          >
            <el-option
              v-for="s in storeOptions"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-col>
      </el-row>
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

    <!-- 趋势对比图 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>营收趋势</span>
              <el-radio-group v-model="revenuePeriod" size="small" @change="loadRevenueTrend">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="revenueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>出租率趋势</span>
              <el-radio-group v-model="occupancyPeriod" size="small" @change="loadOccupancyTrend">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
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
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import StatCard from '@/components/StatCard.vue'
import { getDashboard, getTrendCompare } from '@/api/analysis'
import { getStoreOptions } from '@/api/store'
import * as echarts from 'echarts'

const loading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const selectedStores = ref([])
const storeOptions = ref([])
const revenuePeriod = ref('day')
const occupancyPeriod = ref('day')
const revenueChartRef = ref(null)
const occupancyChartRef = ref(null)
let revenueChart = null
let occupancyChart = null

const data = reactive({
  shouldFill: 0, filled: 0, unfilled: 0,
  totalRevenue: 0, totalRooms: 0, avgOccupancy: 0,
  avgADR: '', avgRevPAR: '',
  revenueRanking: [], occupancyRanking: [], unfilledStores: []
})

function formatMoney(val) {
  if (val == null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function buildStoreIdsParam() {
  return selectedStores.value.length > 0 ? selectedStores.value.join(',') : undefined
}

async function loadStoreOptions() {
  try {
    const res = await getStoreOptions()
    storeOptions.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const params = { date: selectedDate.value }
    const ids = buildStoreIdsParam()
    if (ids) params.storeIds = ids
    const res = await getDashboard(params)
    const d = res.data || {}
    data.shouldFill = d.shouldFill ?? 0
    data.filled = d.filled ?? 0
    data.unfilled = d.unfilled ?? 0
    data.totalRevenue = d.totalRevenue ?? 0
    data.totalRooms = d.totalRooms ?? 0
    data.avgOccupancy = d.avgOccupancy ?? 0
    data.avgADR = d.avgADR ?? ''
    data.avgRevPAR = d.avgRevPAR ?? ''
    data.revenueRanking = d.revenueRanking || []
    data.occupancyRanking = d.occupancyRanking || []
    data.unfilledStores = d.unfilledStores || []
    // 同时加载趋势图
    await loadRevenueTrend()
    await loadOccupancyTrend()
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function loadRevenueTrend() {
  try {
    const params = { date: selectedDate.value, period: revenuePeriod.value, metric: 'revenue' }
    const ids = buildStoreIdsParam()
    if (ids) params.storeIds = ids
    const res = await getTrendCompare(params)
    const d = res.data || {}
    renderTrendChart(revenueChartRef, revenueChart, d, '营收', '¥')
    // 更新chart引用
    if (!revenueChart && revenueChartRef.value) {
      revenueChart = echarts.getInstanceByDom(revenueChartRef.value)
    }
  } catch (e) { /* ignore */ }
}

async function loadOccupancyTrend() {
  try {
    const params = { date: selectedDate.value, period: occupancyPeriod.value, metric: 'occupancy' }
    const ids = buildStoreIdsParam()
    if (ids) params.storeIds = ids
    const res = await getTrendCompare(params)
    const d = res.data || {}
    renderTrendChart(occupancyChartRef, occupancyChart, d, '出租率(%)', '%')
    if (!occupancyChart && occupancyChartRef.value) {
      occupancyChart = echarts.getInstanceByDom(occupancyChartRef.value)
    }
  } catch (e) { /* ignore */ }
}

function renderTrendChart(chartRef, chartInstance, trendData, name, unit) {
  nextTick(() => {
    if (!chartRef.value) return
    if (!chartInstance) {
      chartInstance = echarts.init(chartRef.value)
    }
    const labels = trendData.labels || []
    const current = (trendData.current || []).map(v => v ?? null)
    const lastYear = (trendData.lastYear || []).map(v => v ?? null)

    chartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: function(params) {
          let tip = params[0].axisValue + '<br/>'
          params.forEach(p => {
            const val = p.value != null ? p.value : '-'
            tip += `${p.marker} ${p.seriesName}: ${unit === '¥' ? '¥' : ''}${val}${unit === '%' ? '%' : ''}<br/>`
          })
          return tip
        }
      },
      legend: { data: ['当期', '去年同期'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: labels, boundaryGap: false },
      yAxis: { type: 'value', ...(name.includes('出租率') ? { max: 100 } : {}) },
      series: [
        {
          name: '当期', type: 'line', data: current, smooth: true,
          itemStyle: { color: '#1890ff' }, lineStyle: { width: 2 }
        },
        {
          name: '去年同期', type: 'line', data: lastYear, smooth: true,
          itemStyle: { color: '#bfbfbf' }, lineStyle: { width: 2, type: 'dashed' }
        }
      ]
    }, true)

    // 保存引用
    if (chartRef === revenueChartRef) revenueChart = chartInstance
    if (chartRef === occupancyChartRef) occupancyChart = chartInstance
  })
}

function handleResize() {
  revenueChart?.resize()
  occupancyChart?.resize()
}

onMounted(async () => {
  await loadStoreOptions()
  await loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  revenueChart?.dispose()
  occupancyChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.filter-label {
  display: inline-block;
  margin-right: 8px;
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.stat-row .el-col { margin-bottom: 16px; }

.chart-container { height: 300px; }

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-row { margin-top: 0; }
</style>
