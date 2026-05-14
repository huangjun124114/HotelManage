<template>
  <div class="investor-relation">
    <!-- 投资人选择 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="投资人">
          <el-select v-model="selectedInvestorId" placeholder="请选择投资人" @change="loadRelations" style="width:250px">
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
        <el-table-column prop="investAmount" label="投资金额" width="150" align="right">
          <template #default="{ row }">¥{{ row.investAmount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="shareRatio" label="持股比例" width="100" align="right">
          <template #default="{ row }">{{ row.shareRatio }}%</template>
        </el-table-column>
        <el-table-column prop="investDate" label="投资日期" width="120" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="请选择投资人" />

    <!-- 新增投资关系弹窗 -->
    <el-dialog v-model="dialogVisible" title="添加投资关系" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width:100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
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

const form = reactive({ id: null, storeId: null, investAmount: 0, shareRatio: 0, investDate: '' })

const formRules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  investAmount: [{ required: true, message: '请输入投资金额', trigger: 'blur' }],
  shareRatio: [{ required: true, message: '请输入持股比例', trigger: 'blur' }],
  investDate: [{ required: true, message: '请选择投资日期', trigger: 'change' }]
}

async function loadInvestors() {
  const res = await request({ url: '/investors', method: 'get', params: { size: 999 } })
  investorOptions.value = res.data?.records || []
}

async function loadStores() {
  const res = await request({ url: '/stores/options', method: 'get' })
  storeOptions.value = res.data || []
}

async function loadRelations() {
  if (!selectedInvestorId.value) return
  loading.value = true
  try {
    const res = await request({ url: `/investors/${selectedInvestorId.value}/relations`, method: 'get' })
    relationData.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, { id: null, storeId: null, investAmount: 0, shareRatio: 0, investDate: '' })
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
      data: { ...form, investorId: selectedInvestorId.value }
    })
    ElMessage.success('添加成功')
    dialogVisible.value = false
    loadRelations()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定要删除此投资关系吗？', '提示', { type: 'warning' })
  await request({ url: `/investor-relations/${row.id}`, method: 'delete' })
  ElMessage.success('删除成功')
  loadRelations()
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
