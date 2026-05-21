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
      <el-form-item label="门店名称">
        <el-input v-model="searchForm.storeName" placeholder="请输入" clearable style="width:200px" />
      </el-form-item>
      <el-form-item label="城市">
        <el-input v-model="searchForm.city" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="区域">
        <el-input v-model="searchForm.regionName" placeholder="请输入" clearable style="width:150px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
    </template>

    <template #toolbar>
      <span></span>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增门店</el-button>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="storeCode" label="门店编码" width="120" />
        <el-table-column prop="storeName" label="门店名称" min-width="150" />
        <el-table-column prop="shortName" label="简称" width="100" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="regionName" label="区域" width="100" />
        <el-table-column prop="ownRoomCount" label="房量" width="80" align="right" />
        <el-table-column prop="managerName" label="店长" width="100" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" link @click="handleViewTeam(row)">查看团队</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 新增/编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑门店' : '新增门店'"
    width="600px"
    close-on-click-modal="false"
    @close="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-form-item label="门店编码" prop="storeCode">
        <el-input v-model="form.storeCode" :disabled="isEdit" placeholder="请输入门店编码" />
      </el-form-item>
      <el-form-item label="门店名称" prop="storeName">
        <el-input v-model="form.storeName" placeholder="请输入门店名称" />
      </el-form-item>
      <el-form-item label="简称" prop="shortName">
        <el-input v-model="form.shortName" placeholder="请输入简称" />
      </el-form-item>
      <el-form-item label="城市" prop="city">
        <el-input v-model="form.city" placeholder="请输入城市" />
      </el-form-item>
      <el-form-item label="区域" prop="regionName">
        <el-input v-model="form.regionName" placeholder="请输入区域" />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input v-model="form.address" placeholder="请输入详细地址" />
      </el-form-item>
      <el-form-item label="房量" prop="ownRoomCount">
        <el-input-number v-model="form.ownRoomCount" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="form.contactName" placeholder="请输入联系人" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 团队成员弹层 -->
  <el-dialog
    v-model="teamDialogVisible"
    :title="`${currentStore?.storeName || ''} - 团队成员`"
    width="800px"
    close-on-click-modal="false"
    @close="handleTeamDialogClose"
  >
    <div style="display:flex; justify-content:flex-end; margin-bottom:12px">
      <el-button
        v-if="currentStore?.status !== 0"
        type="primary"
        size="small"
        @click="handleAddMember"
      >
        <el-icon><Plus /></el-icon> 新增人员
      </el-button>
    </div>
    <el-table :data="teamData" border stripe v-loading="teamLoading" style="width:100%">
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="username" label="账号" width="130" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="用户类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.userType === 2 ? 'warning' : 'info'" size="small">
            {{ row.userType === 2 ? '店长' : '员工' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button type="danger" size="small" link @click="handleRemoveMember(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!teamLoading && teamData.length === 0" description="暂无团队成员" />
  </el-dialog>

  <!-- 新增人员弹层（嵌套在团队弹层内，z-index更高） -->
  <el-dialog
    v-model="addMemberDialogVisible"
    title="新增人员"
    width="480px"
    close-on-click-modal="false"
    :append-to-body="true"
    @close="resetMemberForm"
  >
    <el-form ref="memberFormRef" :model="memberForm" :rules="memberFormRules" label-width="90px">
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="memberForm.realName" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="账号" prop="username">
        <el-input v-model="memberForm.username" placeholder="请输入登录账号" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="memberForm.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="初始密码" prop="password">
        <el-input v-model="memberForm.password" placeholder="默认 123456" />
      </el-form-item>
      <el-form-item label="用户类型" prop="userType">
        <el-select v-model="memberForm.userType" style="width:100%">
          <el-option label="店长" :value="2" />
          <el-option label="员工" :value="1" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addMemberDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="memberSubmitLoading" @click="handleMemberSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import { getList, create, update, remove } from '@/api/store'
import { getList as getUserList, create as createUser, update as updateUser } from '@/api/user'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({ storeName: '', city: '', regionName: '', status: null })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])

const form = reactive({
  id: null, storeCode: '', storeName: '', shortName: '', city: '',
  regionName: '', address: '', ownRoomCount: 0, contactName: '', contactPhone: '', status: 1
})

const formRules = {
  storeCode: [{ required: true, message: '请输入门店编码', trigger: 'blur' }],
  storeName: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  shortName: [{ required: true, message: '请输入简称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }]
}

// ---- 团队成员弹层 ----
const teamDialogVisible = ref(false)
const teamLoading = ref(false)
const teamData = ref([])
const currentStore = ref(null)

// ---- 新增人员弹层 ----
const addMemberDialogVisible = ref(false)
const memberSubmitLoading = ref(false)
const memberFormRef = ref(null)
const memberForm = reactive({
  realName: '',
  username: '',
  phone: '',
  password: '123456',
  userType: 1
})
const memberFormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
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
  searchForm.storeName = ''; searchForm.city = ''; searchForm.regionName = ''; searchForm.status = null
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
  Object.assign(form, row)
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null, storeCode: '', storeName: '', shortName: '', city: '',
    regionName: '', address: '', ownRoomCount: 0, contactName: '', contactPhone: '', status: 1
  })
  formRef.value?.resetFields()
}

async function handleSubmit() {
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

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该门店吗？', '提示', { type: 'warning' })
    await remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      const msg = e?.response?.data?.message || e?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

async function handleStatusChange(row, val) {
  try {
    await update({ id: row.id, status: val ? 1 : 0 })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (e) { /* ignore */ }
}

// ---- 团队成员逻辑 ----
async function handleViewTeam(row) {
  currentStore.value = row
  teamDialogVisible.value = true
  await loadTeamData()
}

async function loadTeamData() {
  if (!currentStore.value) return
  teamLoading.value = true
  try {
    const res = await getUserList({ storeId: currentStore.value.id, size: 999 })
    teamData.value = res.data?.records || []
  } catch (e) {
    ElMessage.error('加载团队成员失败')
  } finally {
    teamLoading.value = false
  }
}

function handleTeamDialogClose() {
  currentStore.value = null
  teamData.value = []
}

function handleAddMember() {
  resetMemberForm()
  addMemberDialogVisible.value = true
}

function resetMemberForm() {
  Object.assign(memberForm, {
    realName: '',
    username: '',
    phone: '',
    password: '123456',
    userType: 1
  })
  memberFormRef.value?.resetFields()
}

async function handleMemberSubmit() {
  const valid = await memberFormRef.value.validate().catch(() => false)
  if (!valid) return
  memberSubmitLoading.value = true
  try {
    const roleIds = memberForm.userType === 2 ? [4] : [5]
    await createUser({
      realName: memberForm.realName,
      username: memberForm.username,
      phone: memberForm.phone,
      password: memberForm.password || '123456',
      userType: memberForm.userType,
      storeId: currentStore.value.id,
      roleIds
    })
    ElMessage.success('新增人员成功')
    addMemberDialogVisible.value = false
    await loadTeamData()
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '新增失败'
    ElMessage.error(msg)
  } finally {
    memberSubmitLoading.value = false
  }
}

async function handleRemoveMember(row) {
  try {
    await ElMessageBox.confirm(`确定要将「${row.realName}」从该门店移除吗？`, '提示', { type: 'warning' })
    await updateUser({ id: row.id, storeId: null })
    ElMessage.success('已移除')
    await loadTeamData()
  } catch (e) {
    if (e !== 'cancel') {
      const msg = e?.response?.data?.message || e?.message || '移除失败'
      ElMessage.error(msg)
    }
  }
}

onMounted(() => loadData())
</script>
