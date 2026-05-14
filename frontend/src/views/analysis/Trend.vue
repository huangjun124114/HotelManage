<template>
  <div class="trend" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="门店">
          <el-select v-model="searchForm.storeId" placeholder="请选择门店" clearable @change="loadData" style="width:200px">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
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
            @change="loadData"
            style="width:260px"
          />
        </el-form-item>
        <el-form-item label="指标">
          <el-checkbox-group v-model="indicators" @change="loadData">
            <el-checkbox label="revenue">营收</el-checkbox>
            <el-checkbox label="occupancy">出租率</el-checkbox>
            <el-checkbox label="rooms">间夜数</el-checkbox>
            <el-checkbox label="adr">ADR</el-checkbox>
          </el-checkbox-group>
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
  storeId: null,
  dateRange: (() => {
    const now = new Date()
    const end = now.toISOString().slice(0, 10)
    const start = new Date(now.getTime() - 30 * 86400000).toISOString().slice(0, 10)
    return [start, end]
  })()
})

const indicators = ref(['revenue', 'occupancy'])

function initLineChart() {
  if (!lineChartRef.value || !trendData.value) return
  const chart = echarts.init(lineChartRef.value)
  const dates = trendData.value.dates || []
  const series = indicators.value.map(key => ({
    name: indicatorMap[key]?.name || key,
    type: 'line',
    data: trendData.value[key] || [],
    smooth: true,
    itemStyle: { color: indicatorMap[key]?.color }
  }))

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name) },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series
  })
}

async function loadData() {
  if (!searchForm.storeId) return
  loading.value = true
  try {
    const params = { storeId: searchForm.storeId }
    if (searchForm.dateRange) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getTrend(params)
    trendData.value = res.data || {}
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
