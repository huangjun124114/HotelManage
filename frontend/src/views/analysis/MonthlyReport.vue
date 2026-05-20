<template>
  <div class="monthly-report" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="门店">
          <el-select v-model="searchForm.storeIds" multiple placeholder="全部" clearable collapse-tags collapse-tags-tooltip style="width:280px">
            <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-date-picker
            v-model="searchForm.month"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width:180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="showColumnPicker = true">自定义列</el-button>
          <el-button type="success" @click="exportExcel">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 月度汇总列表 -->
    <el-card shadow="never">
      <template #header>月度门店汇总</template>
      <el-table :data="tableData" border stripe size="small" style="width:100%" show-summary :summary-method="getSummary">
        <el-table-column
          v-for="col in visibleColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.name"
          :width="col.width || ''"
          :align="col.align || 'right'"
        >
          <template #default="{ row }">
            <template v-if="col.prop === 'occupancyRate'">
              {{ row[col.prop] != null ? (Number(row[col.prop]) * 100).toFixed(1) + '%' : '-' }}
            </template>
            <template v-else-if="isMoneyField(col.prop)">
              {{ row[col.prop] != null ? '¥' + Number(row[col.prop]).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2}) : '-' }}
            </template>
            <template v-else>
              {{ row[col.prop] != null ? row[col.prop] : '-' }}
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 列选择器弹窗 -->
    <el-dialog v-model="showColumnPicker" title="自定义列" width="680px">
      <div v-for="group in columnGroups" :key="group.category" class="field-group">
        <div class="group-title">{{ group.category }}</div>
        <el-checkbox-group v-model="selectedColumnKeys">
          <el-checkbox
            v-for="col in group.columns"
            :key="col.prop"
            :label="col.prop"
          >{{ col.name }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="showColumnPicker = false">取消</el-button>
        <el-button type="primary" @click="applyColumns">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getMonthlyDetail } from '@/api/analysis'
import { getStoreOptions } from '@/api/store'
import * as XLSX from 'xlsx'

const loading = ref(false)
const storeOptions = ref([])
const tableData = ref([])
const showColumnPicker = ref(false)

// 所有可选列定义
const allColumns = [
  // 基础
  { prop: 'storeName', name: '门店名称', category: '基础字段', width: 130, align: 'left' },
  { prop: 'reportCount', name: '填报天数', category: '基础字段' },
  { prop: 'ownRoomCount', name: '自有房量', category: '房量与间夜' },
  // 间夜
  { prop: 'roomNights', name: '间夜数', category: '房量与间夜' },
  { prop: 'walkinRoomNights', name: '散客间夜', category: '房量与间夜' },
  { prop: 'ctripRoomNights', name: '携程间夜', category: '房量与间夜' },
  { prop: 'lyRoomNights', name: '同程艺龙间夜', category: '房量与间夜' },
  { prop: 'qunarRoomNights', name: '去哪儿间夜', category: '房量与间夜' },
  { prop: 'zhixingRoomNights', name: '智行间夜', category: '房量与间夜' },
  { prop: 'externalRoomNights', name: '外网间夜', category: '房量与间夜' },
  { prop: 'meituanHotelRoomNights', name: '美团酒店间夜', category: '房量与间夜' },
  { prop: 'fliggyRoomNights', name: '飞猪间夜', category: '房量与间夜' },
  { prop: 'douyinRoomNights', name: '抖音间夜', category: '房量与间夜' },
  { prop: 'xiaozhuRoomNights', name: '小猪间夜', category: '房量与间夜' },
  { prop: 'tujiaRoomNights', name: '途家间夜', category: '房量与间夜' },
  { prop: 'meituanHomestayRoomNights', name: '美团民宿间夜', category: '房量与间夜' },
  { prop: 'jialiRoomNights', name: '加力间夜', category: '房量与间夜' },
  // 经营指标
  { prop: 'occupancyRate', name: '出租率', category: '经营指标' },
  { prop: 'adr', name: 'ADR', category: '经营指标' },
  { prop: 'revpar', name: 'RevPAR', category: '经营指标' },
  // 收入
  { prop: 'dailyRoomFee', name: '日租房房费', category: '收入' },
  { prop: 'hourlyRoomFee', name: '钟点房费用', category: '收入' },
  { prop: 'otherFee', name: '杂费', category: '收入' },
  { prop: 'totalRevenue', name: '总营收', category: '收入' },
  { prop: 'depositAmount', name: '押金', category: '收入' }
]

// 默认选中8列
const defaultColumns = ['storeName', 'ownRoomCount', 'roomNights', 'walkinRoomNights', 'occupancyRate', 'totalRevenue', 'adr', 'revpar']

const selectedColumnKeys = ref([...defaultColumns])

const searchForm = reactive({
  storeIds: [],
  month: new Date().toISOString().slice(0, 7)
})

// 按分类分组的列
const columnGroups = computed(() => {
  const groupMap = {}
  for (const col of allColumns) {
    const cat = col.category || '其他'
    if (!groupMap[cat]) groupMap[cat] = { category: cat, columns: [] }
    groupMap[cat].columns.push(col)
  }
  return Object.values(groupMap)
})

// 可见的列
const visibleColumns = computed(() => {
  return selectedColumnKeys.value
    .map(key => allColumns.find(c => c.prop === key))
    .filter(Boolean)
})

function isMoneyField(prop) {
  return ['dailyRoomFee', 'hourlyRoomFee', 'otherFee', 'totalRevenue', 'depositAmount', 'adr', 'revpar'].includes(prop)
}

async function loadData() {
  if (!searchForm.month) return
  loading.value = true
  try {
    const params = { month: searchForm.month }
    if (searchForm.storeIds && searchForm.storeIds.length > 0) {
      params.storeIds = searchForm.storeIds.join(',')
    }
    const res = await getMonthlyDetail(params)
    tableData.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function applyColumns() {
  showColumnPicker.value = false
}

// 合计行：取最后一条（后端已返回合计行）
function getSummary({ columns, data }) {
  if (!data.length) return []
  const lastRow = data[data.length - 1]
  if (lastRow.storeName !== '合计') return []
  return columns.map(col => {
    const prop = col.property
    if (prop === 'storeName') return '合计'
    if (!prop) return ''
    const val = lastRow[prop]
    if (val == null) return ''
    if (prop === 'occupancyRate') return (Number(val) * 100).toFixed(1) + '%'
    if (isMoneyField(prop)) return '¥' + Number(val).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2})
    return val
  })
}

// Excel导出
async function exportExcel() {
  loading.value = true
  try {
    const params = { month: searchForm.month }
    if (searchForm.storeIds && searchForm.storeIds.length > 0) {
      params.storeIds = searchForm.storeIds.join(',')
    }
    const res = await getMonthlyDetail(params)
    const allData = res.data || []
    if (!allData.length) return

    // 全量导出
    const exportCols = allColumns
    const headers = exportCols.map(c => c.name)
    const rows = allData.map(row => {
      return exportCols.map(col => {
        let val = row[col.prop]
        if (val == null) return ''
        if (col.prop === 'occupancyRate') {
          val = (Number(val) * 100).toFixed(1) + '%'
        }
        return val
      })
    })

    const wsData = [headers, ...rows]
    const ws = XLSX.utils.aoa_to_sheet(wsData)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '月报')
    XLSX.writeFile(wb, `月报_${searchForm.month}.xlsx`)
  } catch (e) {
    console.error('导出失败', e)
  } finally {
    loading.value = false
  }
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

.field-group {
  margin-bottom: 16px;
}

.group-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 4px;
}
</style>
