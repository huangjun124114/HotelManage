<template>
  <div class="investor-relation">
    <!-- 投资人选择 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="投资人">
          <el-select v-model="selectedInvestorId" placeholder="请选择投资人" @change="loadRelations" style="width:280px">
            <el-option v-for="i in investorOptions" :key="i.id" :label="i.name" :value="i.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" v-if="selectedInvestorId">
      <template #header>
        <div class="card-header">
          <span>投资门店关系</span>
          <el-button type="primary" size="small" @click="handleAdd">添加投资关系</el-button>
        </div>
      </template>
      <el-table :data="relationData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="storeName" label="门店名称" min-width="150" />
        <el-table-column prop="investAmount" label="投资金额" width="130" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.investAmount) }}</template>
        </el-table-column>
        <el-table-column prop="investmentRatio" label="持股比例" width="100" align="right">
          <template #default="{ row }">{{ row.investmentRatio }}%</template>
        </el-table-column>
        <el-table-column prop="authStartDate" label="投资日期" width="110" />
        <el-table-column prop="authEndDate" label="撤资日期" width="110">
          <template #default="{ row }">{{ row.authEndDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="relationData.length === 0" description="暂无投资关系" />
    </el-card>

    <el-empty v-else description="请选择投资人" />

    <!-- 新增投资关系弹窗 -->
    <el-dialog v-model="dialogVisible" title="添加投资关系" width="520px" close-on-click-modal="false" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width:100%">
            <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="投资金额" prop="investAmount">
          <el-input-number v-model="form.investAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="持股比例" prop="shareRatio">
          <el-input-number v-model="form.shareRatio" :min="0" :max="100" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="投资日期" prop="investDate">
          <el-date-picker v-model="form.investDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="撤资日期">
          <el-date-picker v-model="form.withdrawDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="未撤资可不填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const selectedInvestorId = ref(null)
const investorOptions = ref([])
const storeOptions = ref([])
const relationData = ref([])

const form = reactive({
  id: null,
  storeId: null,
  investAmount: 0,
  shareRatio: 0,
  investDate: '',
  withdrawDate: ''
})

const formRules = {
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

async function loadInvestors() {
  try {
    const res = await request({ url: '/investors', method: 'get', params: { size: 999 } })
    investorOptions.value = res.data?.records || []
  } catch (e) {
    console.error('加载投资人失败', e)
  }
}

async function loadStores() {
  try {
    const res = await request({ url: '/stores/options', method: 'get' })
    storeOptions.value = res.data || []
  } catch (e) {
    console.error('加载门店失败', e)
  }
}

async function loadRelations() {
  if (!selectedInvestorId.value) return
  loading.value = true
  try {
    const res = await request({
      url: '/investor-relations',
      method: 'get',
      params: { investorId: selectedInvestorId.value }
    })
    relationData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载投资关系失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null,
    storeId: null,
    investAmount: 0,
    shareRatio: 0,
    investDate: '',
    withdrawDate: ''
  })
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await request({
      url: '/investor-relations',
      method: 'post',
      data: {
        investorId: selectedInvestorId.value,
        storeId: form.storeId,
        investAmount: form.investAmount,
        shareRatio: form.shareRatio,
        investDate: form.investDate,
        withdrawDate: form.withdrawDate || null
      }
    })
    ElMessage.success('添加成功')
    dialogVisible.value = false
    loadRelations()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '添加失败'
    ElMessage.error(msg)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除此投资关系吗？', '提示', { type: 'warning' })
    await request({ url: `/investor-relations/${row.id}`, method: 'delete' })
    ElMessage.success('删除成功')
    loadRelations()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadInvestors()
  loadStores()
})
</script>

<style scoped>
.investor-relation {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
