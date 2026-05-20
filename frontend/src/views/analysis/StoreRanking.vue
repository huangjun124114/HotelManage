<template>
  <div class="store-ranking" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="基准日期">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="d => d > new Date()"
            style="width:180px"
          />
        </el-form-item>
        <el-form-item label="时间维度">
          <el-radio-group v-model="period" size="default">
            <el-radio-button label="day">当天</el-radio-button>
            <el-radio-button label="week">当周</el-radio-button>
            <el-radio-button label="month">当月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排名指标">
          <el-select v-model="rankType" style="width:140px">
            <el-option label="营收" value="revenue" />
            <el-option label="出租率" value="occupancy" />
            <el-option label="间夜数" value="rooms" />
            <el-option label="ADR" value="adr" />
            <el-option label="RevPAR" value="revpar" />
            <el-option label="评分" value="score" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>
            <span>门店排名</span>
            <span class="date-range-hint">{{ dateRangeHint }}</span>
          </template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>排名明细</template>
          <el-table :data="rankingData" border stripe size="small" max-height="450">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="storeName" label="门店" min-width="120" />
            <el-table-column :label="rankLabel" width="120" align="right">
              <template #default="{ row }">
                <template v-if="rankType === 'revenue'">¥{{ row.value?.toLocaleString() }}</template>
                <template v-else-if="rankType === 'occupancy'">{{ (row.value * 100).toFixed(1) }}%</template>
                <template v-else-if="rankType === 'adr' || rankType === 'revpar'">¥{{ row.value?.toFixed(2) }}</template>
                <template v-else-if="rankType === 'score'">{{ row.value?.toFixed(2) }}</template>
                <template v-else>{{ row.value }}</template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { getStoreRanking } from '@/api/analysis'
import * as echarts from 'echarts'

const loading = ref(false)
const today = new Date().toISOString().slice(0, 10)
const selectedDate = ref(today)
const period = ref('day')
const rankType = ref('revenue')
const barChartRef = ref(null)
const rankingData = ref([])

// 计算日期范围提示文本
const dateRangeHint = computed(() => {
  if (!selectedDate.value) return ''
  const d = new Date(selectedDate.value)
  if (period.value === 'day') {
    return `（${selectedDate.value}）`
  } else if (period.value === 'week') {
    // 获取本周周一和周日
    const day = d.getDay()
    const diffToMon = day === 0 ? -6 : 1 - day
    const monday = new Date(d)
    monday.setDate(d.getDate() + diffToMon)
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)
    return `（${monday.toISOString().slice(0,10)} 至 ${sunday.toISOString().slice(0,10)}）`
  } else {
    // month
    const firstDay = new Date(d.getFullYear(), d.getMonth(), 1)
    const lastDay = new Date(d.getFullYear(), d.getMonth() + 1, 0)
    return `（${firstDay.toISOString().slice(0,10)} 至 ${lastDay.toISOString().slice(0,10)}）`
  }
})

const rankLabel = computed(() => {
  const map = { revenue: '营收(元)', occupancy: '出租率(%)', rooms: '间夜数', adr: 'ADR(元)', revpar: 'RevPAR(元)', score: '评分' }
  return map[rankType.value] || ''
})

function initBarChart() {
  if (!barChartRef.value || !rankingData.value.length) return
  const chart = echarts.init(barChartRef.value)
  const names = rankingData.value.map(r => r.storeName).reverse()
  const values = rankingData.value.map(r => {
    if (rankType.value === 'occupancy') return (r.value * 100).toFixed(1)
    return r.value
  }).reverse()

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: { color: '#1890ff' }
    }]
  })
}

async function loadData() {
  if (!selectedDate.value) return
  loading.value = true
  try {
    const params = {
      date: selectedDate.value,
      period: period.value,
      metric: rankType.value
    }
    const res = await getStoreRanking(params)
    rankingData.value = res.data || []
    nextTick(() => initBarChart())
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

// 监听维度变化，自动查询
watch(period, () => loadData())

onMounted(() => loadData())
</script>

<style scoped>
.store-ranking {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  height: 450px;
}

.date-range-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
