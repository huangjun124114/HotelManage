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
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ row.submitTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
            <el-button type="warning" size="small" link @click="handleLock(row)">锁定</el-button>
            <el-button type="danger" size="small" link @click="handleReject(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 退回弹窗 -->
  <el-dialog v-model="rejectVisible" title="退回日报" width="450px">
    <el-form>
      <el-form-item label="退回原因">
        <el-input v-model="rejectReason" type="textarea" rows="3" placeholder="请输入退回原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectVisible = false">取消</el-button>
      <el-button type="primary" @click="handleRejectConfirm">确认退回</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageLayout from '@/components/PageLayout.vue'
import { queryList, lock, reject, getDetail } from '@/api/report'
import { getStoreOptions } from '@/api/store'

const loading = ref(false)
const searchForm = reactive({ storeId: null, dateRange: null, status: 1 })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const storeOptions = ref([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, status: 1, ...searchForm }
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
  searchForm.storeId = null; searchForm.dateRange = null
  page.value = 1; loadData()
}
function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }

async function handleView(row) {
  await getDetail(row.id)
  // 可打开详情弹窗
}

async function handleLock(row) {
  await ElMessageBox.confirm('锁定后将无法修改，确认锁定？', '提示', { type: 'warning' })
  await lock(row.id)
  row.status = 2
  ElMessage.success('已锁定')
}

function handleReject(row) {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value) {
    ElMessage.warning('请输入退回原因')
    return
  }
  await reject(currentRow.value.id, rejectReason.value)
  currentRow.value.status = 3
  rejectVisible.value = false
  ElMessage.success('已退回')
}

onMounted(async () => {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []
  loadData()
})
</script>
