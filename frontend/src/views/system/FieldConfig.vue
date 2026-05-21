<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>字段配置</span>
        <el-select v-model="selectedTemplateId" placeholder="请选择模板" @change="loadFields" style="width:250px">
          <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
        </el-select>
      </div>
    </template>
    <el-empty v-if="!selectedTemplateId" description="请先选择模板" />
    <template v-else>
      <el-table :data="fields" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="fieldCode" label="字段编码" width="150" />
        <el-table-column prop="fieldName" label="字段名称" min-width="150" />
        <el-table-column prop="fieldCategory" label="分组" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.fieldType === 'number' || row.fieldType === '1' ? 'primary' : row.fieldType === 'text' || row.fieldType === '2' ? '' : 'warning'">
              {{ row.fieldType === 'number' || row.fieldType === '1' ? '数字' : row.fieldType === 'text' || row.fieldType === '2' ? '文本' : '计算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.required === 1 ? 'danger' : 'info'" size="small">
              {{ row.required === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="只读" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.readonlyFlag === 1 ? 'warning' : 'success'" size="small">
              {{ row.readonlyFlag === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="70" align="center" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEditField(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </el-card>

  <!-- 字段编辑弹窗 -->
  <el-dialog v-model="fieldDialogVisible" title="编辑字段" width="500px" close-on-click-modal="false">
    <el-form :model="fieldForm" label-width="80px">
      <el-form-item label="字段编码">
        <el-input v-model="fieldForm.fieldCode" disabled />
      </el-form-item>
      <el-form-item label="字段名称">
        <el-input v-model="fieldForm.fieldName" />
      </el-form-item>
      <el-form-item label="分组">
        <el-input v-model="fieldForm.fieldCategory" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="fieldForm.sortNo" :min="0" />
      </el-form-item>
      <el-form-item label="必填">
        <el-switch v-model="fieldForm.required" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="只读">
        <el-switch v-model="fieldForm.readonlyFlag" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="fieldDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="fieldSubmitLoading" @click="handleSaveField">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const fieldSubmitLoading = ref(false)
const templates = ref([])
const selectedTemplateId = ref(null)
const fields = ref([])
const fieldDialogVisible = ref(false)
const fieldForm = reactive({
  id: null, fieldCode: '', fieldName: '', fieldCategory: '',
  sortNo: 0, required: 0, readonlyFlag: 0
})

async function loadTemplates() {
  const res = await request({ url: '/templates', method: 'get', params: { size: 999 } })
  templates.value = res.data?.records || []
}

async function loadFields() {
  if (!selectedTemplateId.value) return
  loading.value = true
  try {
    const res = await request({ url: `/templates/${selectedTemplateId.value}/fields`, method: 'get' })
    fields.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleEditField(row) {
  Object.assign(fieldForm, {
    id: row.id, fieldCode: row.fieldCode, fieldName: row.fieldName,
    fieldCategory: row.fieldCategory, sortNo: row.sortNo || 0,
    required: row.required || 0, readonlyFlag: row.readonlyFlag || 0
  })
  fieldDialogVisible.value = true
}

async function handleSaveField() {
  fieldSubmitLoading.value = true
  try {
    await request({ url: `/templates/fields/${fieldForm.id}`, method: 'put', data: { ...fieldForm } })
    ElMessage.success('修改成功')
    fieldDialogVisible.value = false
    loadFields()
  } catch (e) { /* ignore */ }
  finally { fieldSubmitLoading.value = false }
}

onMounted(() => loadTemplates())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
