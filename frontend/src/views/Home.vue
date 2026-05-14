<template>
  <div class="home-container" v-loading="loading">
    <!-- CEO/总部首页 (userType=1) -->
    <template v-if="userStore.userInfo?.userType === 1">
      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="4">
          <StatCard title="门店总数" :value="homeData.totalStores" icon="Shop" color="#1890ff" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="4">
          <StatCard title="已填报" :value="homeData.filledCount" icon="Checked" color="#52c41a" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="4">
          <StatCard title="未填报" :value="homeData.unfilledCount" icon="Warning" color="#ff4d4f" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="4">
          <StatCard title="今日总营收" :value="formatMoney(homeData.totalRevenue)" icon="Money" color="#722ed1" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="4">
          <StatCard title="今日总间夜" :value="homeData.totalRooms" icon="House" color="#fa8c16" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="4">
          <StatCard title="平均出租率" :value="homeData.avgOccupancy + '%'" icon="TrendCharts" color="#13c2c2" />
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
            <template #header>门店营收排名 Top10</template>
            <el-table :data="homeData.revenueRanking || []" size="small">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="storeName" label="门店" />
              <el-table-column prop="revenue" label="营收">
                <template #default="{ row }">¥{{ formatMoney(row.revenue) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card>
            <template #header>门店出租率排名 Top10</template>
            <el-table :data="homeData.occupancyRanking || []" size="small">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="storeName" label="门店" />
              <el-table-column prop="occupancy" label="出租率">
                <template #default="{ row }">{{ row.occupancy }}%</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 未填报门店 -->
      <el-card class="section-card" v-if="homeData.unfilledStores?.length">
        <template #header>未填报门店</template>
        <el-table :data="homeData.unfilledStores" size="small">
          <el-table-column prop="storeName" label="门店名称" />
          <el-table-column prop="city" label="城市" />
          <el-table-column prop="managerName" label="店长" />
        </el-table>
      </el-card>
    </template>

    <!-- 门店店长首页 (userType=2) -->
    <template v-if="userStore.userInfo?.userType === 2">
      <!-- 今日填报状态 -->
      <el-card class="status-card">
        <el-result
          v-if="homeData.todaySubmitted"
          icon="success"
          title="今日日报已提交"
          sub-title="如需修改请联系管理员"
        >
          <template #extra>
            <el-button type="primary" @click="goToReportQuery">查看日报</el-button>
          </template>
        </el-result>
        <el-result
          v-else
          icon="warning"
          title="今日日报未填报"
          sub-title="请尽快填写今日经营数据"
        >
          <template #extra>
            <el-button type="primary" @click="goToReportFill">立即填报</el-button>
          </template>
        </el-result>
      </el-card>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard title="本月累计营收" :value="formatMoney(homeData.monthlyRevenue)" icon="Money" color="#1890ff" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard title="本月间夜数" :value="homeData.monthlyRooms" icon="House" color="#52c41a" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard title="本月平均出租率" :value="homeData.monthlyOccupancy + '%'" icon="TrendCharts" color="#fa8c16" />
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard title="本月ADR" :value="'¥' + homeData.monthlyADR" icon="PieChart" color="#722ed1" />
        </el-col>
      </el-row>

      <!-- 经营趋势图 -->
      <el-card class="section-card">
        <template #header>最近7日经营趋势</template>
        <div ref="storeTrendChartRef" class="chart-container"></div>
      </el-card>

      <!-- 快捷入口 -->
      <el-card class="section-card">
        <template #header>快捷入口</template>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="6">
            <div class="quick-link" @click="goToReportFill">
              <el-icon :size="28" color="#1890ff"><Edit /></el-icon>
              <span>日报填报</span>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="quick-link" @click="goToReportQuery">
              <el-icon :size="28" color="#52c41a"><Search /></el-icon>
              <span>历史日报</span>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </template>

    <!-- 投资者首页 (userType=3) -->
    <template v-if="userStore.userInfo?.userType === 3">
      <el-card class="section-card">
        <template #header>我的投资门店</template>
        <el-table :data="homeData.investStores || []" size="small">
          <el-table-column prop="storeName" label="门店名称" />
          <el-table-column prop="city" label="城市" />
          <el-table-column label="今日营收">
            <template #default="{ row }">¥{{ formatMoney(row.todayRevenue) }}</template>
          </el-table-column>
          <el-table-column label="本月累计营收">
            <template #default="{ row }">¥{{ formatMoney(row.monthlyRevenue) }}</template>
          </el-table-column>
          <el-table-column label="今日出租率">
            <template #default="{ row }">{{ row.occupancy }}%</template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="viewStoreTrend(row.storeId)">查看趋势</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="section-card" v-if="selectedStoreId">
        <template #header>门店经营趋势</template>
        <div ref="investTrendChartRef" class="chart-container"></div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getDashboard, getTrend } from '@/api/analysis'
import { getToday } from '@/api/report'
import StatCard from '@/components/StatCard.vue'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const homeData = reactive({
  totalStores: 0,
  filledCount: 0,
  unfilledCount: 0,
  totalRevenue: 0,
  totalRooms: 0,
  avgOccupancy: 0,
  revenueRanking: [],
  occupancyRanking: [],
  unfilledStores: [],
  todaySubmitted: false,
  monthlyRevenue: 0,
  monthlyRooms: 0,
  monthlyOccupancy: 0,
  monthlyADR: 0,
  investStores: []
})

const revenueChartRef = ref(null)
const occupancyChartRef = ref(null)
const storeTrendChartRef = ref(null)
const investTrendChartRef = ref(null)
const selectedStoreId = ref(null)

let revenueChart = null
let occupancyChart = null
let storeTrendChart = null
let investTrendChart = null

function formatMoney(val) {
  if (val == null) return '¥0'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function goToReportFill() {
  router.push('/report/fill')
}

function goToReportQuery() {
  router.push('/report/query')
}

function viewStoreTrend(storeId) {
  selectedStoreId.value = storeId
  nextTick(() => loadInvestTrend())
}

function initChart(chartRef, title, dates, series) {
  if (!chartRef.value) return null
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name) },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series: series.map(s => ({
      name: s.name,
      type: 'line',
      data: s.data,
      smooth: true
    }))
  })
  return chart
}

function loadCharts() {
  if (userStore.userInfo?.userType === 1) {
    revenueChart = initChart(revenueChartRef, '营收', homeData.dateLabels || [],
      [{ name: '营收', data: homeData.revenueTrend || [] }])
    occupancyChart = initChart(occupancyChartRef, '出租率', homeData.dateLabels || [],
      [{ name: '出租率(%)', data: homeData.occupancyTrend || [] }])
  } else if (userStore.userInfo?.userType === 2) {
    storeTrendChart = initChart(storeTrendChartRef, '经营趋势', homeData.dateLabels || [],
      [
        { name: '营收(元)', data: homeData.storeRevenueTrend || [] },
        { name: '出租率(%)', data: homeData.storeOccupancyTrend || [] }
      ])
  }
}

async function loadInvestTrend() {
  if (!selectedStoreId.value || !investTrendChartRef.value) return
  try {
    const res = await getTrend({ storeId: selectedStoreId.value, days: 7 })
    const data = res.data || {}
    investTrendChart = initChart(investTrendChartRef, '经营趋势', data.dates || [],
      [
        { name: '营收(元)', data: data.revenueTrend || [] },
        { name: '出租率(%)', data: data.occupancyTrend || [] }
      ])
  } catch (e) {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    if (userStore.userInfo?.userType === 1 || userStore.userInfo?.userType === 3) {
      const res = await getDashboard({ userType: userStore.userInfo.userType })
      const data = res.data || {}
      homeData.totalStores = data.totalStores ?? 0
      homeData.filledCount = data.filledCount ?? 0
      homeData.unfilledCount = data.unfilledCount ?? 0
      homeData.totalRevenue = data.totalRevenue ?? 0
      homeData.totalRooms = data.totalRooms ?? 0
      homeData.avgOccupancy = data.avgOccupancy ?? 0
      homeData.revenueRanking = data.revenueRanking || []
      homeData.occupancyRanking = data.occupancyRanking || []
      homeData.unfilledStores = data.unfilledStores || []
      homeData.dateLabels = data.dateLabels || []
      homeData.revenueTrend = data.revenueTrend || []
      homeData.occupancyTrend = data.occupancyTrend || []
      homeData.investStores = data.investStores || []
    } else if (userStore.userInfo?.userType === 2) {
      // 店长获取今日日报状态和本月数据
      const todayRes = await getToday({ storeId: userStore.userInfo.storeIds?.[0] })
      homeData.todaySubmitted = todayRes.data != null
      homeData.monthlyRevenue = todayRes.data?.monthlyRevenue ?? 0
      homeData.monthlyRooms = todayRes.data?.monthlyRooms ?? 0
      homeData.monthlyOccupancy = todayRes.data?.monthlyOccupancy ?? 0
      homeData.monthlyADR = todayRes.data?.monthlyADR ?? 0
    }
    nextTick(() => loadCharts())
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.home-container {
  padding: 0;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-row .el-col {
  margin-bottom: 16px;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-container {
  height: 300px;
}

.table-row {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
}

.status-card {
  margin-bottom: 16px;
  text-align: center;
}

.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
  cursor: pointer;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.3s;
}

.quick-link:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.quick-link span {
  font-size: 14px;
}
</style>
