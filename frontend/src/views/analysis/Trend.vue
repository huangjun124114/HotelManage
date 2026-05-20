<template>
  <div class="trend" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="门店">
          <el-select v-model="searchForm.storeIds" multiple placeholder="全部" clearable collapse-tags collapse-tags-tooltip style="width:280px">
            <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width:260px"
          />
          <DateQuickSelect v-model="searchForm.dateRange" style="margin-left:8px" />
        </el-form-item>
        <el-form-item label="时间维度">
          <el-radio-group v-model="searchForm.period">
            <el-radio-button label="day">天</el-radio-button>
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="指标">
          <el-checkbox-group v-model="indicators">
            <el-checkbox label="revenue">营收</el-checkbox>
            <el-checkbox label="occupancy">出租率</el-checkbox>
            <el-checkbox label="rooms">间夜数</el-checkbox>
            <el-checkbox label="adr">ADR</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>趋势分析</template>
      <div ref="lineChartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getTrend } from '@/api/analysis'
import { getStoreOptions } from '@/api/store'
import DateQuickSelect from '@/components/DateQuickSelect.vue'
import * as echarts from 'echarts'

const loading = ref(false)
const storeOptions = ref([])
const lineChartRef = ref(null)
const trendData = ref(null)

const indicatorMap = {
  revenue: { name: '营收(元)', color: '#1890ff' },
  occupancy: { name: '出租率(%)', color: '#52c41a' },
  rooms: { name: '间夜数', color: '#fa8c16' },
  adr: { name: 'ADR(元)', color: '#722ed1' }
}

const searchForm = reactive({
  storeIds: [],
  dateRange: (() => {
    const now = new Date()
    const end = now.toISOString().slice(0, 10)
    const start = new Date(now.getTime() - 30 * 86400000).toISOString().slice(0, 10)
    return [start, end]
  })(),
  period: 'day'
})

const indicators = ref(['revenue', 'occupancy'])

let chartInstance = null

function initLineChart() {
  if (!lineChartRef.value || !trendData.value || !trendData.value.length) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(lineChartRef.value)
  const dates = trendData.value.map(d => d.date)
  const series = indicators.value.map(key => {
    const info = indicatorMap[key] || { name: key, color: '#999' }
    const data = trendData.value.map(d => {
      if (key === 'revenue') return d.revenue
      if (key === 'occupancy') return d.occupancyRate != null ? (d.occupancyRate * 100).toFixed(2) : 0
      if (key === 'rooms') return d.roomNights
      if (key === 'adr') return d.adr
      return 0
    })
    return {
      name: info.name,
      type: 'line',
      data,
      smooth: true,
      itemStyle: { color: info.color }
    }
  })

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name) },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series
  })
}

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (searchForm.storeIds && searchForm.storeIds.length > 0) {
      params.storeIds = searchForm.storeIds.join(',')
    }
    if (searchForm.dateRange) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    params.period = searchForm.period
    const res = await getTrend(params)
    trendData.value = res.data || []
    nextTick(() => initLineChart())
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

onMounted(async () => {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []
})
</script>

<style scoped>
.trend {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  height: 400px;
}
</style>
