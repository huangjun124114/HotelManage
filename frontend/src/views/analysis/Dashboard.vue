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
            style="width: 100%"
          />
        </el-col>
        <el-col :xs="24" :sm="10" :md="8">
          <span class="filter-label">快捷日期</span>
          <el-radio-group v-model="quickDate" size="small" @change="onQuickDateChange">
            <el-radio-button label="today">今天</el-radio-button>
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
          </el-radio-group>
        </el-col>
        <el-col :xs="24" :sm="16" :md="8">
          <span class="filter-label">门店</span>
          <el-select
            v-model="selectedStores"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部门店"
            clearable
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
        <el-col :xs="24" :sm="6" :md="2">
          <el-button type="primary" @click="loadData" style="width: 100%">查询</el-button>
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

    <!-- 趋势对比图 - 第一行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>营收趋势</span>
              <el-radio-group v-model="periodState.revenue" size="small" @change="() => loadTrend('revenue')">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div :ref="el => chartRefs.revenue = el" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>出租率趋势</span>
              <el-radio-group v-model="periodState.occupancy" size="small" @change="() => loadTrend('occupancy')">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div :ref="el => chartRefs.occupancy = el" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>ADR趋势</span>
              <el-radio-group v-model="periodState.adr" size="small" @change="() => loadTrend('adr')">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div :ref="el => chartRefs.adr = el" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势对比图 - 第二行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>RevPAR趋势</span>
              <el-radio-group v-model="periodState.revpar" size="small" @change="() => loadTrend('revpar')">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div :ref="el => chartRefs.revpar = el" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>间夜数趋势</span>
              <el-radio-group v-model="periodState.roomnights" size="small" @change="() => loadTrend('roomnights')">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div :ref="el => chartRefs.roomnights = el" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>填报率趋势</span>
              <el-radio-group v-model="periodState.fillrate" size="small" @change="() => loadTrend('fillrate')">
                <el-radio-button label="day">天</el-radio-button>
                <el-radio-button label="week">周</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div :ref="el => chartRefs.fillrate = el" class="chart-container"></div>
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
const quickDate = ref('today')

// 快捷日期切换
function onQuickDateChange(val) {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  const d = now.getDate()
  const dayOfWeek = now.getDay() || 7 // 周一=1, 周日=7

  if (val === 'today') {
    selectedDate.value = `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  } else if (val === 'week') {
    // 本周一
    const monday = new Date(now)
    monday.setDate(d - dayOfWeek + 1)
    selectedDate.value = monday.toISOString().slice(0, 10)
  } else if (val === 'month') {
    // 本月1号
    selectedDate.value = `${y}-${String(m + 1).padStart(2, '0')}-01`
  }
}

// 6个趋势图各自的周期状态
const periodState = reactive({
  revenue: 'day',
  occupancy: 'day',
  adr: 'day',
  revpar: 'day',
  roomnights: 'day',
  fillrate: 'day'
})

// 图表DOM引用（用普通对象，不用reactive包裹ref，否则Vue3自动解包导致ref绑定失效）
const chartRefs = {
  revenue: null,
  occupancy: null,
  adr: null,
  revpar: null,
  roomnights: null,
  fillrate: null
}

// 图表实例
const chartInstances = {}

const data = reactive({
  shouldFill: 0, filled: 0, unfilled: 0,
  totalRevenue: 0, totalRooms: 0, avgOccupancy: 0,
  avgADR: '', avgRevPAR: '',
  revenueRanking: [], occupancyRanking: [], unfilledStores: []
})

// 图表配置映射
const chartConfig = {
  revenue:    { name: '营收',       unit: '¥', prefix: '¥', suffix: '',  yMax: null },
  occupancy:  { name: '出租率(%)',  unit: '%', prefix: '',  suffix: '%', yMax: 100 },
  adr:        { name: 'ADR(¥)',     unit: '¥', prefix: '¥', suffix: '',  yMax: null },
  revpar:     { name: 'RevPAR(¥)',  unit: '¥', prefix: '¥', suffix: '',  yMax: null },
  roomnights: { name: '间夜数',     unit: '',  prefix: '',  suffix: '',  yMax: null },
  fillrate:   { name: '填报率(%)',  unit: '%', prefix: '',  suffix: '%', yMax: 100 }
}

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
    // 加载全部6个趋势图
    await Promise.all([
      loadTrend('revenue'),
      loadTrend('occupancy'),
      loadTrend('adr'),
      loadTrend('revpar'),
      loadTrend('roomnights'),
      loadTrend('fillrate')
    ])
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function loadTrend(metric) {
  try {
    const params = {
      date: selectedDate.value,
      period: periodState[metric],
      metric: metric
    }
    const ids = buildStoreIdsParam()
    if (ids) params.storeIds = ids
    const res = await getTrendCompare(params)
    const d = res.data || {}
    renderTrendChart(metric, d)
  } catch (e) { /* ignore */ }
}

function renderTrendChart(metric, trendData) {
  nextTick(() => {
    const domRef = chartRefs[metric]
    if (!domRef) return

    // 初始化或获取已有实例
    if (!chartInstances[metric]) {
      chartInstances[metric] = echarts.init(domRef)
    }
    const chart = chartInstances[metric]
    const config = chartConfig[metric]

    const labels = trendData.labels || []
    const current = (trendData.current || []).map(v => v ?? null)
    const lastYear = (trendData.lastYear || []).map(v => v ?? null)

    chart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: function(params) {
          let tip = params[0].axisValue + '<br/>'
          params.forEach(p => {
            const val = p.value != null ? p.value : '-'
            tip += `${p.marker} ${p.seriesName}: ${config.prefix}${val}${config.suffix}<br/>`
          })
          return tip
        }
      },
      legend: { data: ['当期', '去年同期'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: labels, boundaryGap: false },
      yAxis: {
        type: 'value',
        ...(config.yMax ? { max: config.yMax } : {})
      },
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
  })
}

function handleResize() {
  Object.values(chartInstances).forEach(chart => chart?.resize())
}

onMounted(async () => {
  await loadStoreOptions()
  await loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances).forEach(chart => chart?.dispose())
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

.chart-container { height: 280px; }

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-row { margin-top: 0; }
</style>
