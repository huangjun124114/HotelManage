<template>
  <div class="store-ranking" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="日期">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            @change="loadData"
          />
        </el-form-item>
        <el-form-item label="排名指标">
          <el-select v-model="rankType" @change="loadData" style="width:140px">
            <el-option label="营收" value="revenue" />
            <el-option label="出租率" value="occupancy" />
            <el-option label="间夜数" value="rooms" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>门店排名</template>
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
                <template v-else-if="rankType === 'occupancy'">{{ row.value }}%</template>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { getStoreRanking } from '@/api/analysis'
import * as echarts from 'echarts'

const loading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const rankType = ref('revenue')
const barChartRef = ref(null)
const rankingData = ref([])

const rankLabel = computed(() => {
  const map = { revenue: '营收(元)', occupancy: '出租率(%)', rooms: '间夜数' }
  return map[rankType.value] || ''
})

function initBarChart() {
  if (!barChartRef.value || !rankingData.value.length) return
  const chart = echarts.init(barChartRef.value)
  const names = rankingData.value.map(r => r.storeName).reverse()
  const values = rankingData.value.map(r => r.value).reverse()

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
  loading.value = true
  try {
    const res = await getStoreRanking({ date: selectedDate.value, type: rankType.value })
    rankingData.value = res.data || []
    nextTick(() => initBarChart())
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

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
</style>
