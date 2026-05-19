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
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PageLayout>

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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageLayout from '@/components/PageLayout.vue'
import request from '@/utils/request'

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

const form = reactive({ id: null, name: '', phone: '', company: '', remark: '' })

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await request({ url: '/investors', method: 'get', params: { ...searchForm, page: page.value, size: size.value } })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
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
      await request({ url: `/investors/${form.id}`, method: 'put', data: { ...form } })
      ElMessage.success('修改成功')
      dialogVisible.value = false
      loadData()
    } else {
      const res = await request({ url: '/investors', method: 'post', data: { ...form } })
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadData()
      // 展示自动创建的账户信息
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

onMounted(() => loadData())
</script>
