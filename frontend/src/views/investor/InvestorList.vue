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
      <el-form-item label="姓名">
        <el-input v-model="searchForm.investorName" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:100px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
    </template>

    <template #toolbar>
      <span></span>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增投资人</el-button>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="company" label="公司" min-width="150" />
        <el-table-column prop="investStoreCount" label="投资门店数" width="100" align="center" />
        <el-table-column prop="totalInvest" label="投资总额" width="120" align="right">
          <template #default="{ row }">¥{{ row.totalInvest?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" link @click="handleAddInvest(row)">增加投资</el-button>
            <el-button type="info" size="small" link @click="handleViewRecords(row)">查看投资记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 新增/编辑投资人弹窗 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑投资人' : '新增投资人'" width="500px" close-on-click-modal="false" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="公司" prop="company">
        <el-input v-model="form.company" placeholder="请输入公司" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 增加投资弹层 -->
  <el-dialog
    v-model="addInvestDialogVisible"
    title="增加投资"
    width="520px"
    close-on-click-modal="false"
    :append-to-body="true"
    @close="resetAddInvestForm"
  >
    <el-form ref="addInvestFormRef" :model="addInvestForm" :rules="addInvestFormRules" label-width="90px">
      <el-form-item label="门店" prop="storeId">
        <el-select v-model="addInvestForm.storeId" placeholder="请选择门店" style="width:100%" filterable>
          <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="投资金额" prop="investAmount">
        <el-input-number v-model="addInvestForm.investAmount" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="持股比例(%)" prop="shareRatio">
        <el-input-number v-model="addInvestForm.shareRatio" :min="0" :max="100" :precision="2" style="width:100%" placeholder="如输入30表示30%" />
      </el-form-item>
      <el-form-item label="投资日期" prop="investDate">
        <el-date-picker v-model="addInvestForm.investDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
      </el-form-item>
      <el-form-item label="撤资日期">
        <el-date-picker v-model="addInvestForm.withdrawDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="未撤资可不填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addInvestDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="addInvestLoading" @click="handleAddInvestSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 查看投资记录弹层 -->
  <el-dialog
    v-model="investRecordDialogVisible"
    :title="`${currentInvestor?.name || ''} - 投资记录`"
    width="900px"
    close-on-click-modal="false"
    :append-to-body="true"
    @close="handleRecordDialogClose"
  >
    <el-table :data="relationData" border stripe v-loading="relationLoading" style="width:100%">
      <el-table-column prop="storeName" label="门店名称" min-width="140" />
      <el-table-column label="投资金额" width="130" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.investAmount) }}</template>
      </el-table-column>
      <el-table-column label="持股比例" width="100" align="right">
        <template #default="{ row }">{{ formatRatio(row.investmentRatio) }}</template>
      </el-table-column>
      <el-table-column prop="authStartDate" label="投资日期" width="110" />
      <el-table-column label="撤资日期" width="110">
        <template #default="{ row }">{{ row.authEndDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleEditRelation(row)">编辑</el-button>
          <el-button type="danger" size="small" link @click="handleDeleteRelation(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!relationLoading && relationData.length === 0" description="暂无投资记录" />
  </el-dialog>

  <!-- 编辑投资关系弹层（嵌套） -->
  <el-dialog
    v-model="editRelationDialogVisible"
    title="编辑投资关系"
    width="520px"
    close-on-click-modal="false"
    :append-to-body="true"
    @close="resetEditRelationForm"
  >
    <el-form ref="editRelationFormRef" :model="editRelationForm" :rules="editRelationFormRules" label-width="90px">
      <el-form-item label="门店" prop="storeId">
        <el-select v-model="editRelationForm.storeId" placeholder="请选择门店" style="width:100%" filterable>
          <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="投资金额" prop="investAmount">
        <el-input-number v-model="editRelationForm.investAmount" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="持股比例(%)" prop="shareRatio">
        <el-input-number v-model="editRelationForm.shareRatio" :min="0" :max="100" :precision="2" style="width:100%" placeholder="如输入30表示30%" />
      </el-form-item>
      <el-form-item label="投资日期" prop="investDate">
        <el-date-picker v-model="editRelationForm.investDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
      </el-form-item>
      <el-form-item label="撤资日期">
        <el-date-picker v-model="editRelationForm.withdrawDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="未撤资可不填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editRelationDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="editRelationLoading" @click="handleEditRelationSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import { getStoreOptions } from '@/api/store'
import {
  getInvestorList, createInvestor, updateInvestor,
  getRelationList, createRelation, updateRelation, deleteRelation
} from '@/api/investor'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const searchForm = reactive({ investorName: '', phone: '', status: null })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const storeOptions = ref([])

const form = reactive({ id: null, name: '', phone: '', company: '', remark: '' })

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

// ---- 增加投资弹层 ----
const addInvestDialogVisible = ref(false)
const addInvestLoading = ref(false)
const addInvestFormRef = ref(null)
const currentInvestor = ref(null)
const addInvestForm = reactive({
  storeId: null,
  investAmount: 0,
  shareRatio: 0,
  investDate: '',
  withdrawDate: ''
})
const addInvestFormRules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  investAmount: [{ required: true, message: '请输入投资金额', trigger: 'blur' }],
  shareRatio: [{ required: true, message: '请输入持股比例', trigger: 'blur' }],
  investDate: [{ required: true, message: '请选择投资日期', trigger: 'change' }]
}

// ---- 查看投资记录弹层 ----
const investRecordDialogVisible = ref(false)
const relationLoading = ref(false)
const relationData = ref([])

// ---- 编辑投资关系弹层 ----
const editRelationDialogVisible = ref(false)
const editRelationLoading = ref(false)
const editRelationFormRef = ref(null)
const editingRelationId = ref(null)
const editRelationForm = reactive({
  storeId: null,
  investAmount: 0,
  shareRatio: 0,
  investDate: '',
  withdrawDate: ''
})
const editRelationFormRules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  investAmount: [{ required: true, message: '请输入投资金额', trigger: 'blur' }],
  shareRatio: [{ required: true, message: '请输入持股比例', trigger: 'blur' }],
  investDate: [{ required: true, message: '请选择投资日期', trigger: 'change' }]
}

function formatAmount(val) {
  if (val === null || val === undefined) return '-'
  const num = Number(val)
  return isNaN(num) ? '-' : num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRatio(val) {
  if (val === null || val === undefined) return '-'
  const num = Number(val)
  if (isNaN(num)) return '-'
  // 数据库存的是百分比值（如30.00表示30%），直接显示+%
  return num.toFixed(2).replace(/\.?0+$/, '') + '%'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getInvestorList({ ...searchForm, page: page.value, size: size.value })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function loadStoreOptions() {
  try {
    const res = await getStoreOptions()
    storeOptions.value = res.data || []
  } catch (e) { /* ignore */ }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() {
  searchForm.investorName = ''; searchForm.phone = ''; searchForm.status = null
  page.value = 1; loadData()
}
function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }
function handleAdd() { isEdit.value = false; resetForm(); dialogVisible.value = true }
function handleEdit(row) { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
function resetForm() { Object.assign(form, { id: null, name: '', phone: '', company: '', remark: '' }); formRef.value?.resetFields() }

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateInvestor(form.id, { ...form })
      ElMessage.success('修改成功')
      dialogVisible.value = false
      loadData()
    } else {
      const res = await createInvestor({ ...form })
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadData()
      const data = res.data
      if (data && data.password) {
        ElNotification({
          title: '投资人账户已创建',
          message: `用户名：${data.username}  初始密码：${data.password}`,
          type: 'success',
          duration: 0,
          showClose: true
        })
      }
    }
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '操作失败'
    ElMessage.error(msg)
  } finally {
    submitLoading.value = false
  }
}

// ---- 增加投资逻辑 ----
function handleAddInvest(row) {
  currentInvestor.value = row
  resetAddInvestForm()
  addInvestDialogVisible.value = true
}

function resetAddInvestForm() {
  Object.assign(addInvestForm, {
    storeId: null,
    investAmount: 0,
    shareRatio: 0,
    investDate: '',
    withdrawDate: ''
  })
  addInvestFormRef.value?.resetFields()
}

async function handleAddInvestSubmit() {
  const valid = await addInvestFormRef.value.validate().catch(() => false)
  if (!valid) return
  addInvestLoading.value = true
  try {
    await createRelation({
      investorId: currentInvestor.value.id,
      storeId: addInvestForm.storeId,
      investAmount: addInvestForm.investAmount,
      shareRatio: addInvestForm.shareRatio,
      investDate: addInvestForm.investDate,
      withdrawDate: addInvestForm.withdrawDate || null
    })
    ElMessage.success('投资记录已添加')
    addInvestDialogVisible.value = false
    loadData()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    addInvestLoading.value = false
  }
}

// ---- 查看投资记录逻辑 ----
async function handleViewRecords(row) {
  currentInvestor.value = row
  investRecordDialogVisible.value = true
  await loadRelationData()
}

async function loadRelationData() {
  if (!currentInvestor.value) return
  relationLoading.value = true
  try {
    const res = await getRelationList(currentInvestor.value.id)
    relationData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载投资记录失败')
  } finally {
    relationLoading.value = false
  }
}

function handleRecordDialogClose() {
  relationData.value = []
  currentInvestor.value = null
}

async function handleDeleteRelation(row) {
  try {
    await ElMessageBox.confirm('确定要删除此投资关系吗？', '提示', { type: 'warning' })
    await deleteRelation(row.id)
    ElMessage.success('删除成功')
    await loadRelationData()
  } catch (e) {
    if (e !== 'cancel') {
      const msg = e?.response?.data?.message || e?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

// ---- 编辑投资关系逻辑 ----
function handleEditRelation(row) {
  editingRelationId.value = row.id
  Object.assign(editRelationForm, {
    storeId: row.storeId || null,
    investAmount: row.investAmount ? Number(row.investAmount) : 0,
    // GET 返回 investmentRatio，编辑时映射到 shareRatio 字段
    shareRatio: row.investmentRatio ? Number(row.investmentRatio) : 0,
    // GET 返回 authStartDate，编辑时映射到 investDate 字段
    investDate: row.authStartDate || '',
    // GET 返回 authEndDate，编辑时映射到 withdrawDate 字段
    withdrawDate: row.authEndDate || ''
  })
  editRelationDialogVisible.value = true
}

function resetEditRelationForm() {
  Object.assign(editRelationForm, {
    storeId: null,
    investAmount: 0,
    shareRatio: 0,
    investDate: '',
    withdrawDate: ''
  })
  editRelationFormRef.value?.resetFields()
}

async function handleEditRelationSubmit() {
  const valid = await editRelationFormRef.value.validate().catch(() => false)
  if (!valid) return
  editRelationLoading.value = true
  try {
    await updateRelation(editingRelationId.value, {
      storeId: editRelationForm.storeId,
      investAmount: editRelationForm.investAmount,
      shareRatio: editRelationForm.shareRatio,
      investDate: editRelationForm.investDate,
      withdrawDate: editRelationForm.withdrawDate || null
    })
    ElMessage.success('修改成功')
    editRelationDialogVisible.value = false
    await loadRelationData()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '修改失败'
    ElMessage.error(msg)
  } finally {
    editRelationLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadStoreOptions()
})
</script>
