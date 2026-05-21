<template>
  <PageLayout :show-search="false" :total="total" :page="page" :size="size"
    @size-change="handleSizeChange" @page-change="handlePageChange">
    <template #toolbar>
      <span></span>
      <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增模板</el-button>
    </template>
    <template #table>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="templateName" label="模板名称" min-width="150" />
        <el-table-column prop="templateCode" label="模板编码" width="150" />
        <el-table-column label="是否默认" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" size="small">
              {{ row.isDefault === 1 ? '是' : '否' }}
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
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

  <!-- 新增/编辑弹窗 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="500px" close-on-click-modal="false" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="form.templateName" placeholder="请输入模板名称" />
      </el-form-item>
      <el-form-item label="模板编码" prop="templateCode">
        <el-input v-model="form.templateCode" placeholder="请输入模板编码" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="适用类型" prop="applyType">
        <el-select v-model="form.applyType" placeholder="请选择" style="width:100%">
          <el-option label="全部门店" :value="0" />
          <el-option label="指定门店" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否默认">
        <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import PageLayout from '@/components/PageLayout.vue'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])

const form = reactive({
  id: null, templateName: '', templateCode: '', applyType: 0,
  isDefault: 0, status: 1, remark: ''
})

const formRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await request({ url: '/templates', method: 'get', params: { page: page.value, size: size.value } })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
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
    id: row.id, templateName: row.templateName, templateCode: row.templateCode,
    applyType: row.applyType || 0, isDefault: row.isDefault || 0,
    status: row.status, remark: row.remark || ''
  })
  dialogVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null, templateName: '', templateCode: '', applyType: 0,
    isDefault: 0, status: 1, remark: ''
  })
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await request({ url: `/templates/${form.id}`, method: 'put', data: { ...form } })
      ElMessage.success('修改成功')
    } else {
      await request({ url: '/templates', method: 'post', data: { ...form } })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定要删除模板「${row.templateName}」吗？关联字段也会一并删除。`, '提示', { type: 'warning' })
  await request({ url: `/templates/${row.id}`, method: 'delete' })
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
