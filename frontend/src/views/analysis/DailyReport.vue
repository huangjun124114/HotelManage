<template>
  <div class="daily-report" v-loading="loading">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="门店">
          <el-select v-model="searchForm.storeId" placeholder="全部" clearable style="width:200px">
            <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="showColumnPicker = true">自定义列</el-button>
          <el-button type="success" @click="exportExcel">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 动态列表格 -->
    <el-card shadow="never">
      <template #header>日报数据</template>
      <el-table :data="tableData" border stripe size="small" style="width:100%" max-height="600">
        <el-table-column
          v-for="col in visibleColumns"
          :key="col.fieldCode"
          :prop="col.fieldCode"
          :label="col.fieldName"
          :width="col.width || ''"
          :align="col.align || 'right'"
        >
          <template #default="{ row }">
            <template v-if="col.fieldCode === 'occupancyRate'">
              {{ row[col.fieldCode] != null ? (row[col.fieldCode] * 100).toFixed(1) + '%' : '-' }}
            </template>
            <template v-else-if="isMoneyField(col.fieldCode)">
              {{ row[col.fieldCode] != null ? '¥' + Number(row[col.fieldCode]).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2}) : '-' }}
            </template>
            <template v-else-if="isRateField(col.fieldCode)">
              {{ row[col.fieldCode] != null ? (Number(row[col.fieldCode]) * 100).toFixed(1) + '%' : '-' }}
            </template>
            <template v-else-if="isIntegerField(col.fieldCode)">
              {{ row[col.fieldCode] != null ? row[col.fieldCode] : '-' }}
            </template>
            <template v-else>
              {{ row[col.fieldCode] != null ? row[col.fieldCode] : '-' }}
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 0"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="searchForm.pageSize"
        :current-page="searchForm.pageNo"
        :page-sizes="[20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 列选择器弹窗 -->
    <el-dialog v-model="showColumnPicker" title="自定义列" width="680px">
      <div v-for="group in fieldGroups" :key="group.category" class="field-group">
        <div class="group-title">{{ group.category }}</div>
        <el-checkbox-group v-model="selectedFieldCodes">
          <el-checkbox
            v-for="field in group.fields"
            :key="field.fieldCode"
            :label="field.fieldCode"
          >{{ field.fieldName }}</el-checkbox>
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
import { getStoreOptions } from '@/api/store'
import { queryList, getTemplateFields } from '@/api/report'
import * as XLSX from 'xlsx'

const loading = ref(false)
const storeOptions = ref([])
const tableData = ref([])
const total = ref(0)
const showColumnPicker = ref(false)

// 字段列表（从模板接口获取）
const allFields = ref([])
const selectedFieldCodes = ref([])

// 字段映射：fieldCode -> DailyReport属性名
const fieldCodeToProp = {
  storeName: 'storeName',
  reportDate: 'reportDate',
  own_room_count: 'ownRoomCount',
  hourly_room_count: 'hourlyRoomCount',
  repair_room_count: 'repairRoomCount',
  room_nights: 'roomNights',
  walkin_room_nights: 'walkinRoomNights',
  ctrip_room_nights: 'ctripRoomNights',
  ly_room_nights: 'lyRoomNights',
  qunar_room_nights: 'qunarRoomNights',
  zhixing_room_nights: 'zhixingRoomNights',
  external_room_nights: 'externalRoomNights',
  meituan_hotel_room_nights: 'meituanHotelRoomNights',
  fliggy_room_nights: 'fliggyRoomNights',
  douyin_room_nights: 'douyinRoomNights',
  xiaozhu_room_nights: 'xiaozhuRoomNights',
  tujia_room_nights: 'tujiaRoomNights',
  meituan_homestay_room_nights: 'meituanHomestayRoomNights',
  jiali_room_nights: 'jialiRoomNights',
  occupancy_rate: 'occupancyRate',
  adr: 'adr',
  revpar: 'revpar',
  daily_room_fee: 'dailyRoomFee',
  hourly_room_fee: 'hourlyRoomFee',
  other_fee: 'otherFee',
  total_revenue: 'totalRevenue',
  deposit_amount: 'depositAmount',
  ctrip_scan_count: 'ctripScanCount',
  meituan_scan_count: 'meituanScanCount'
}

// 默认选中的8列
const defaultFields = ['storeName', 'reportDate', 'own_room_count', 'room_nights', 'walkin_room_nights', 'occupancy_rate', 'total_revenue', 'revpar']

const searchForm = reactive({
  storeId: null,
  dateRange: (() => {
    const now = new Date()
    const end = now.toISOString().slice(0, 10)
    const start = new Date(now.getTime() - 7 * 86400000).toISOString().slice(0, 10)
    return [start, end]
  })(),
  pageNo: 1,
  pageSize: 20
})

// 按fieldCategory分组的字段
const fieldGroups = computed(() => {
  const groupMap = {}
  for (const field of allFields.value) {
    // 只支持DailyReport主表已有的字段
    if (!fieldCodeToProp[field.fieldCode]) continue
    const cat = field.fieldCategory || '其他'
    if (!groupMap[cat]) groupMap[cat] = { category: cat, fields: [] }
    groupMap[cat].fields.push(field)
  }
  return Object.values(groupMap)
})

// 可见的列
const visibleColumns = computed(() => {
  return selectedFieldCodes.value
    .map(code => {
      const field = allFields.value.find(f => f.fieldCode === code)
      if (!field) return null
      const prop = fieldCodeToProp[code] || code
      return {
        fieldCode: prop,
        fieldName: field.fieldName,
        width: ['storeName'].includes(prop) ? 130 : ['reportDate'].includes(prop) ? 110 : '',
        align: ['storeName', 'reportDate'].includes(prop) ? 'left' : 'right'
      }
    })
    .filter(Boolean)
})

// 判断是否为金额字段
function isMoneyField(code) {
  return ['dailyRoomFee', 'hourlyRoomFee', 'otherFee', 'totalRevenue', 'depositAmount', 'adr', 'revpar'].includes(code)
}

// 判断是否为比率字段
function isRateField(code) {
  return ['occupancyRate'].includes(code)
}

// 判断是否为整数类型字段
function isIntegerField(code) {
  return ['ownRoomCount', 'hourlyRoomCount', 'repairRoomCount', 'ctripScanCount', 'meituanScanCount'].includes(code)
}

async function loadFields() {
  try {
    const res = await getTemplateFields(1)
    const fields = res.data || []
    // 过滤只保留主表有的字段
    allFields.value = fields.filter(f => fieldCodeToProp[f.fieldCode])
    // 默认选中
    if (selectedFieldCodes.value.length === 0) {
      selectedFieldCodes.value = defaultFields.filter(code =>
        allFields.value.some(f => f.fieldCode === code)
      )
    }
  } catch (e) {
    console.error('加载字段失败', e)
    // 使用硬编码字段
    allFields.value = [
      { fieldCode: 'storeName', fieldName: '门店名称', fieldCategory: '基础字段' },
      { fieldCode: 'reportDate', fieldName: '日期', fieldCategory: '基础字段' },
      { fieldCode: 'own_room_count', fieldName: '自有房量', fieldCategory: '房量与间夜' },
      { fieldCode: 'room_nights', fieldName: '间夜数', fieldCategory: '房量与间夜' },
      { fieldCode: 'walkin_room_nights', fieldName: '散客', fieldCategory: '房量与间夜' },
      { fieldCode: 'occupancy_rate', fieldName: '出租率', fieldCategory: '经营指标' },
      { fieldCode: 'total_revenue', fieldName: '当日总营收', fieldCategory: '收入' },
      { fieldCode: 'revpar', fieldName: 'RevPAR', fieldCategory: '经营指标' }
    ]
    selectedFieldCodes.value = defaultFields.filter(code =>
      allFields.value.some(f => f.fieldCode === code)
    )
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNo: searchForm.pageNo,
      pageSize: searchForm.pageSize
    }
    if (searchForm.storeId) params.storeId = searchForm.storeId
    if (searchForm.dateRange && searchForm.dateRange[0]) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await queryList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSizeChange(size) {
  searchForm.pageSize = size
  searchForm.pageNo = 1
  loadData()
}

function handlePageChange(page) {
  searchForm.pageNo = page
  loadData()
}

function applyColumns() {
  showColumnPicker.value = false
}

// Excel全量导出
async function exportExcel() {
  loading.value = true
  try {
    // 查询全部数据（不分页）
    const params = { pageNo: 1, pageSize: 10000 }
    if (searchForm.storeId) params.storeId = searchForm.storeId
    if (searchForm.dateRange && searchForm.dateRange[0]) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await queryList(params)
    const allData = res.data?.records || []
    if (!allData.length) {
      return
    }

    // 全量字段表头
    const exportFieldMap = [
      { prop: 'storeName', name: '门店名称' },
      { prop: 'reportDate', name: '日期' },
      { prop: 'ownRoomCount', name: '自有房量' },
      { prop: 'hourlyRoomCount', name: '钟点房数量' },
      { prop: 'repairRoomCount', name: '维修房' },
      { prop: 'roomNights', name: '间夜数' },
      { prop: 'walkinRoomNights', name: '散客间夜' },
      { prop: 'ctripRoomNights', name: '携程间夜' },
      { prop: 'lyRoomNights', name: '同程艺龙间夜' },
      { prop: 'qunarRoomNights', name: '去哪儿间夜' },
      { prop: 'zhixingRoomNights', name: '智行间夜' },
      { prop: 'externalRoomNights', name: '外网间夜' },
      { prop: 'meituanHotelRoomNights', name: '美团酒店间夜' },
      { prop: 'fliggyRoomNights', name: '飞猪间夜' },
      { prop: 'douyinRoomNights', name: '抖音间夜' },
      { prop: 'xiaozhuRoomNights', name: '小猪间夜' },
      { prop: 'tujiaRoomNights', name: '途家间夜' },
      { prop: 'meituanHomestayRoomNights', name: '美团民宿间夜' },
      { prop: 'jialiRoomNights', name: '加力间夜' },
      { prop: 'occupancyRate', name: '出租率' },
      { prop: 'adr', name: 'ADR' },
      { prop: 'revpar', name: 'RevPAR' },
      { prop: 'dailyRoomFee', name: '日租房房费' },
      { prop: 'hourlyRoomFee', name: '钟点房费用' },
      { prop: 'otherFee', name: '杂费' },
      { prop: 'totalRevenue', name: '当日总营收' },
      { prop: 'depositAmount', name: '押金' },
      { prop: 'ctripScanCount', name: '携程扫码' },
      { prop: 'meituanScanCount', name: '美团扫码' }
    ]

    const headers = exportFieldMap.map(f => f.name)
    const rows = allData.map(row => {
      return exportFieldMap.map(f => {
        let val = row[f.prop]
        if (val == null) return ''
        // 出租率转为百分比
        if (f.prop === 'occupancyRate') {
          val = (Number(val) * 100).toFixed(1) + '%'
        }
        return val
      })
    })

    const wsData = [headers, ...rows]
    const ws = XLSX.utils.aoa_to_sheet(wsData)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '日报')
    XLSX.writeFile(wb, `日报_${searchForm.dateRange ? searchForm.dateRange[0] + '_' + searchForm.dateRange[1] : 'export'}.xlsx`)
  } catch (e) {
    console.error('导出失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []
  await loadFields()
  loadData()
})
</script>

<style scoped>
.daily-report {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
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
