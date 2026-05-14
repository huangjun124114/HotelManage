<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>字段配置</span>
        <el-select v-model="selectedTemplateId" placeholder="请选择模板" @change="loadFields" style="width:200px">
          <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>
    </template>
    <el-empty v-if="!selectedTemplateId" description="请先选择模板" />
    <template v-else>
      <el-table :data="fields" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="fieldName" label="字段名" width="150" />
        <el-table-column prop="label" label="显示名称" min-width="150" />
        <el-table-column prop="groupName" label="分组" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.fieldType === 1 ? '数字' : row.fieldType === 2 ? '文本' : '计算' }}</template>
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
            <el-tag :type="row.readonly === 1 ? 'warning' : 'success'" size="small">
              {{ row.readonly === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
      </el-table>
    </template>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const templates = ref([])
const selectedTemplateId = ref(null)
const fields = ref([])

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

onMounted(() => loadTemplates())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
