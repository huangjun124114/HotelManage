<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>菜单管理</span>
        <el-button type="primary" size="small" @click="handleAdd(null)"><el-icon><Plus /></el-icon> 新增目录</el-button>
      </div>
    </template>
    <el-table :data="menuTree" row-key="id" border stripe v-loading="loading" default-expand-all>
      <el-table-column prop="menuName" label="名称" min-width="180" />
      <el-table-column prop="menuCode" label="编码" width="160" />
      <el-table-column prop="path" label="路径" width="160" />
      <el-table-column prop="permissionCode" label="权限标识" width="180" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.menuType === 1 ? '' : row.menuType === 2 ? 'success' : 'warning'" size="small">
            {{ row.menuType === 1 ? '目录' : row.menuType === 2 ? '菜单' : '按钮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortNo" label="排序" width="70" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="handleToggleStatus(row)"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.menuType !== 3" type="success" size="small" link @click="handleAdd(row)">新增子项</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新增/编辑弹窗 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="550px" close-on-click-modal="false" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="上级菜单">
        <el-input :value="parentName || '顶级'" disabled />
      </el-form-item>
      <el-form-item label="菜单类型" prop="menuType">
        <el-radio-group v-model="form.menuType">
          <el-radio :value="1">目录</el-radio>
          <el-radio :value="2">菜单</el-radio>
          <el-radio :value="3">按钮</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单名称" prop="menuName">
        <el-input v-model="form.menuName" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="菜单编码" prop="menuCode">
        <el-input v-model="form.menuCode" placeholder="请输入编码" />
      </el-form-item>
      <el-form-item v-if="form.menuType !== 3" label="路由路径" prop="path">
        <el-input v-model="form.path" placeholder="如 /system/user" />
      </el-form-item>
      <el-form-item v-if="form.menuType === 2" label="组件路径" prop="component">
        <el-input v-model="form.component" placeholder="如 system/UserList" />
      </el-form-item>
      <el-form-item label="权限标识" prop="permissionCode">
        <el-input v-model="form.permissionCode" placeholder="如 system:user" />
      </el-form-item>
      <el-form-item label="排序" prop="sortNo">
        <el-input-number v-model="form.sortNo" :min="0" :max="999" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTree, create, update, remove } from '@/api/menu'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const menuTree = ref([])
const parentName = ref('')
const parentId = ref(null)

const form = reactive({
  id: null, parentId: null, menuName: '', menuCode: '', menuType: 2,
  path: '', component: '', permissionCode: '', sortNo: 0, status: 1
})

const formRules = {
  menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTree()
    menuTree.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleAdd(parent) {
  isEdit.value = false
  resetForm()
  if (parent) {
    parentId.value = parent.id
    parentName.value = parent.menuName
    // 如果父级是目录，默认子级为菜单；如果父级是菜单，默认子级为按钮
    form.menuType = parent.menuType === 1 ? 2 : 3
  } else {
    parentId.value = 0
    parentName.value = ''
    form.menuType = 1
  }
  form.parentId = parentId.value
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, parentId: row.parentId, menuName: row.menuName, menuCode: row.menuCode,
    menuType: row.menuType, path: row.path || '', component: row.component || '',
    permissionCode: row.permissionCode || '', sortNo: row.sortNo || 0, status: row.status
  })
  parentName.value = ''
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null, parentId: null, menuName: '', menuCode: '', menuType: 2,
    path: '', component: '', permissionCode: '', sortNo: 0, status: 1
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

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const text = newStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确定要${text}菜单「${row.menuName}」吗？`, '提示', { type: 'warning' })
  await update({ id: row.id, status: newStatus, menuName: row.menuName, menuCode: row.menuCode, menuType: row.menuType, parentId: row.parentId })
  ElMessage.success(`${text}成功`)
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定要删除菜单「${row.menuName}」吗？子菜单也会一并删除。`, '提示', { type: 'warning' })
  await remove(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
