<template>
  <PageLayout
    :show-search="false"
    :total="total"
    :page="page"
    :size="size"
    @size-change="handleSizeChange"
    @page-change="handlePageChange"
  >
    <template #toolbar>
      <span></span>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增角色</el-button>
    </template>

    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="name" label="角色名称" min-width="150" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" link @click="handleAssignMenu(row)">分配菜单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 新增/编辑弹窗 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" placeholder="请输入编码" />
      </el-form-item>
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入名称" />
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

  <!-- 菜单分配弹窗 -->
  <el-dialog v-model="menuDialogVisible" title="分配菜单" width="400px">
    <el-tree
      ref="menuTreeRef"
      :data="menuTree"
      show-checkbox
      node-key="id"
      :default-checked-keys="checkedMenuIds"
      :props="{ children: 'children', label: 'name' }"
      default-expand-all
    />
    <template #footer>
      <el-button @click="menuDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="menuLoading" @click="handleSaveMenus">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import { getList, getById, create, update, getRoleMenus, saveRoleMenus } from '@/api/role'
import { getTree } from '@/api/menu'

const loading = ref(false)
const submitLoading = ref(false)
const menuLoading = ref(false)
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const menuTreeRef = ref(null)

const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const menuTree = ref([])
const checkedMenuIds = ref([])
const currentRoleId = ref(null)

const form = reactive({ id: null, code: '', name: '', status: 1 })

const formRules = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getList({ page: page.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function loadMenuTree() {
  const res = await getTree()
  menuTree.value = res.data || []
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
  Object.assign(form, { id: null, code: '', name: '', status: 1 })
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

async function handleAssignMenu(row) {
  currentRoleId.value = row.id
  const res = await getRoleMenus(row.id)
  checkedMenuIds.value = res.data || []
  menuDialogVisible.value = true
}

async function handleSaveMenus() {
  const checkedKeys = menuTreeRef.value.getCheckedKeys()
  const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys()
  const allKeys = [...checkedKeys, ...halfCheckedKeys]
  menuLoading.value = true
  try {
    await saveRoleMenus(currentRoleId.value, allKeys)
    ElMessage.success('菜单分配成功')
    menuDialogVisible.value = false
  } catch (e) { /* ignore */ }
  finally { menuLoading.value = false }
}

onMounted(() => {
  loadData()
  loadMenuTree()
})
</script>
