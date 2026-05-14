<template>
  <PageLayout
    :search-form="searchForm"
    :total="total"
    :page="page"
    :size="size"
    @search="handleSearch"
    @reset="handleReset"
    @size-change="handleSizeChange"
    @page-change="handlePageChange"
  >
    <template #search>
      <el-form-item label="门店">
        <el-select v-model="searchForm.storeId" placeholder="全部" clearable style="width:200px">
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
          style="width:260px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
          <el-option label="草稿" :value="0" />
          <el-option label="已提交" :value="1" />
          <el-option label="已锁定" :value="2" />
          <el-option label="已退回" :value="3" />
        </el-select>
      </el-form-item>
    </template>

    <template #toolbar>
      <span></span>
      <el-button @click="handleExport">
        <el-icon><Download /></el-icon> 导出Excel
      </el-button>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="storeName" label="门店" min-width="130" />
        <el-table-column prop="reportDate" label="日期" width="110" />
        <el-table-column prop="rooms" label="间夜数" width="80" align="right" />
        <el-table-column label="出租率" width="80" align="right">
          <template #default="{ row }">{{ row.occupancy }}%</template>
        </el-table-column>
        <el-table-column label="ADR" width="90" align="right">
          <template #default="{ row }">¥{{ row.adr?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="总营收" width="120" align="right">
          <template #default="{ row }">¥{{ row.totalRevenue?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : row.status === 3 ? 'danger' : 'info'"
              size="small"
            >
              {{ row.status === 1 ? '已提交' : row.status === 2 ? '已锁定' : row.status === 3 ? '已退回' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" link @click="handleLock(row)">锁定</el-button>
            <el-button v-if="row.status === 2" type="warning" size="small" link @click="handleUnlock(row)">解锁</el-button>
            <el-button v-if="row.status === 1" type="danger" size="small" link @click="handleReject(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" title="日报详情" width="800px">
    <el-descriptions v-if="detailData" :column="3" border>
      <el-descriptions-item label="门店">{{ detailData.storeName }}</el-descriptions-item>
      <el-descriptions-item label="日期">{{ detailData.reportDate }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="detailData.status === 1 ? 'success' : 'info'" size="small">
          {{ detailData.status === 1 ? '已提交' : '草稿' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 退回弹窗 -->
  <el-dialog v-model="rejectVisible" title="退回日报" width="450px">
    <el-form>
      <el-form-item label="退回原因">
        <el-input v-model="rejectReason" type="textarea" rows="3" placeholder="请输入退回原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectVisible = false">取消</el-button>
      <el-button type="primary" @click="handleRejectSubmit">确认退回</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import { queryList, lock, unlock, reject, getDetail } from '@/api/report'
import { getStoreOptions } from '@/api/store'

const loading = ref(false)
const searchForm = reactive({ storeId: null, dateRange: null, status: null })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const storeOptions = ref([])

const detailVisible = ref(false)
const detailData = ref(null)
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)

async function loadStoreOpts() {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, ...searchForm }
    if (params.dateRange) {
      params.startDate = params.dateRange[0]
      params.endDate = params.dateRange[1]
      delete params.dateRange
    }
    const res = await queryList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() {
  searchForm.storeId = null; searchForm.dateRange = null; searchForm.status = null
  page.value = 1; loadData()
}
function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }

async function handleView(row) {
  const res = await getDetail(row.id)
  detailData.value = res.data
  detailVisible.value = true
}

async function handleLock(row) {
  await ElMessageBox.confirm('锁定后将无法修改，确认锁定？', '提示', { type: 'warning' })
  await lock(row.id)
  row.status = 2
  ElMessage.success('已锁定')
}

async function handleUnlock(row) {
  await ElMessageBox.confirm('确认解锁该日报？', '提示', { type: 'warning' })
  await unlock(row.id)
  row.status = 1
  ElMessage.success('已解锁')
}

function handleReject(row) {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectSubmit() {
  if (!rejectReason.value) {
    ElMessage.warning('请输入退回原因')
    return
  }
  await reject(currentRow.value.id, rejectReason.value)
  currentRow.value.status = 3
  rejectVisible.value = false
  ElMessage.success('已退回')
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadStoreOpts()
  loadData()
})
</script>
