<template>
  <div class="channel-analysis" v-loading="loading">
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
            @change="loadData"
            style="width:260px"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>渠道间夜占比</template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>渠道间夜明细</template>
          <el-table :data="channelData" border stripe size="small">
            <el-table-column prop="channelName" label="渠道" />
            <el-table-column prop="roomNights" label="间夜数" align="right" />
            <el-table-column label="占比" width="100" align="right">
              <template #default="{ row }">{{ (row.ratio * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="房费" width="120" align="right">
              <template #default="{ row }">¥{{ row.roomFee?.toLocaleString() }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getChannelAnalysis } from '@/api/analysis'
import { getStoreOptions } from '@/api/store'
import * as echarts from 'echarts'

const loading = ref(false)
const storeOptions = ref([])
const pieChartRef = ref(null)
const channelData = ref([])

const searchForm = reactive({
  storeIds: [],
  dateRange: (() => {
    const now = new Date()
    const end = now.toISOString().slice(0, 10)
    const start = new Date(now.getTime() - 7 * 86400000).toISOString().slice(0, 10)
    return [start, end]
  })()
})

function initPieChart() {
  if (!pieChartRef.value || !channelData.value.length) return
  const chart = echarts.init(pieChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 10, top: 20 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      data: channelData.value.map(c => ({ name: c.channelName, value: c.roomNights })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  })
}

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (searchForm.storeIds && searchForm.storeIds.length > 0) {
      params.storeId = searchForm.storeIds.join(',')
    }
    if (searchForm.dateRange) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getChannelAnalysis(params)
    channelData.value = res.data || []
    nextTick(() => initPieChart())
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
.channel-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  height: 350px;
}
</style>
