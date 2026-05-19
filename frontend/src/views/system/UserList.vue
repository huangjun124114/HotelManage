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
        <el-input v-model="searchForm.realName" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="searchForm.phone" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="用户类型">
        <el-select v-model="searchForm.userType" placeholder="全部" clearable style="width:120px">
          <el-option label="总部" :value="1" />
          <el-option label="店长" :value="2" />
          <el-option label="投资人" :value="3" />
        </el-select>
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
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增用户</el-button>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ row.userType === 1 ? '总部' : row.userType === 2 ? '店长' : '投资人' }}</template>
        </el-table-column>
        <el-table-column prop="storeNames" label="门店" min-width="150">
          <template #default="{ row }">{{ row.storeNames?.join('、') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="roleNames" label="角色" width="150">
          <template #default="{ row }">{{ row.roleNames?.join('、') || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" size="small" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              link
              @click="handleToggleStatus(row)"
            >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 新增/编辑弹窗 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="550px" close-on-click-modal="false" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="账号" prop="username">
        <el-input v-model="form.username" placeholder="请输入账号" :disabled="isEdit" />
      </el-form-item>
      <el-form-item v-if="!isEdit" label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="用户类型" prop="userType">
        <el-select v-model="form.userType" placeholder="请选择" style="width:100%">
          <el-option label="总部" :value="1" />
          <el-option label="店长" :value="2" />
          <el-option label="投资人" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属门店" prop="storeIds">
        <el-select v-model="form.storeIds" multiple placeholder="请选择门店" style="width:100%">
          <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色" prop="roleIds">
        <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width:100%">
          <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 重置密码弹窗 -->
  <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px" close-on-click-modal="false">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
      <el-form-item label="新密码" prop="password">
        <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleResetPwdSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import { getList, create, update, resetPassword, updateStatus } from '@/api/user'
import { getStoreOptions } from '@/api/store'
import { getList as getRoleList } from '@/api/role'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const pwdDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const pwdFormRef = ref(null)
const storeOptions = ref([])
const roleOptions = ref([])

const searchForm = reactive({ realName: '', phone: '', userType: null, status: null })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const currentRow = ref(null)

const form = reactive({
  id: null, username: '', password: '', realName: '', phone: '',
  userType: 2, storeIds: [], roleIds: []
})

const pwdForm = reactive({ password: '' })

const formRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
}

const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' }
  ]
}

async function loadOptions() {
  const [storeRes, roleRes] = await Promise.all([getStoreOptions(), getRoleList({ size: 999 })])
  storeOptions.value = storeRes.data || []
  roleOptions.value = roleRes.data?.records || roleRes.data || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await getList({ ...searchForm, page: page.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleSearch() { page.value = 1; loadData() }
function handleReset() {
  searchForm.realName = ''; searchForm.phone = ''; searchForm.userType = null; searchForm.status = null
  page.value = 1; loadData()
}
function handleSizeChange(val) { size.value = val; loadData() }
function handlePageChange(val) { page.value = val; loadData() }

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, username: row.username, password: '', realName: row.realName,
    phone: row.phone, userType: row.userType,
    storeIds: row.storeIds || [], roleIds: row.roleIds || []
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null, username: '', password: '', realName: '', phone: '',
    userType: 2, storeIds: [], roleIds: []
  })
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const rules = isEdit.value
    ? { username: formRules.username, realName: formRules.realName, userType: formRules.userType }
    : { ...formRules, password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码长度不少于6位' }] }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await update({ ...form })
      ElMessage.success('修改成功')
    } else {
      await create({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

function handleResetPwd(row) {
  currentRow.value = row
  pwdForm.password = ''
  pwdDialogVisible.value = true
}

async function handleResetPwdSubmit() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  await resetPassword(currentRow.value.id, { password: pwdForm.password })
  ElMessage.success('密码重置成功')
  pwdDialogVisible.value = false
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const text = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定要${text}该用户吗？`, '提示', { type: 'warning' })
  await updateStatus(row.id, newStatus)
  row.status = newStatus
  ElMessage.success(`${text}成功`)
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>
