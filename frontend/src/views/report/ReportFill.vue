<template>
  <div class="report-fill" v-loading="pageLoading">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="top-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="24" :sm="8">
          <el-form-item label="门店" label-width="60px">
            <el-select
              v-model="selectedStoreId"
              placeholder="请选择门店"
              @change="loadTodayReport"
              :disabled="!canSelectStore"
              style="width:100%"
            >
              <el-option
                v-for="s in storeOptions"
                :key="s.id"
                :label="s.name"
                :value="s.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="日期" label-width="60px">
            <el-date-picker
              v-model="reportDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              @change="loadTodayReport"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8" class="action-col">
          <el-button type="primary" @click="handleSaveDraft" :loading="saveLoading">保存草稿</el-button>
          <el-button type="success" @click="handleSubmit" :loading="submitLoading">提交</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 模板选择（仅多模板时显示） -->
    <el-card shadow="never" class="template-card" v-if="templates.length > 1">
      <el-form-item label="模板" label-width="60px">
        <el-select v-model="selectedTemplateId" @change="loadTemplateFields" style="width:250px">
          <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
    </el-card>

    <!-- 动态表单 -->
    <div v-if="fields.length > 0">
      <el-card
        v-for="group in groupedFields"
        :key="group.name"
        shadow="never"
        class="field-group-card"
      >
        <template #header>
          <span class="group-title">{{ group.name }}</span>
        </template>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="140px">
          <el-row :gutter="16">
            <el-col
              v-for="field in group.fields"
              :key="field.fieldName"
              :xs="24"
              :sm="12"
              :lg="8"
            >
              <el-form-item
                :label="field.label"
                :prop="field.fieldName"
                :required="field.required === 1"
              >
                <!-- 只读/计算字段 -->
                <el-input
                  v-if="field.readonlyFlag === 1 || field.readonly === 1"
                  :model-value="computedValues[field.fieldName] ?? '-'"
                  disabled
                  style="background:#f5f5f5"
                />
                <!-- 数字类型 -->
                <el-input-number
                  v-else-if="field.fieldType === 'number' || field.fieldType === 1"
                  v-model="formData[field.fieldName]"
                  :min="0"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  style="width:100%"
                  @change="handleFieldChange(field.fieldName)"
                />
                <!-- 文本类型 -->
                <el-input
                  v-else
                  v-model="formData[field.fieldName]"
                  :placeholder="'请输入' + field.label"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
    </div>

    <el-empty v-else-if="!pageLoading" description="暂无表单字段" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getStoreOptions } from '@/api/store'
import { getToday, getTemplateFields, saveDraft, submit } from '@/api/report'
import request from '@/utils/request'

const userStore = useUserStore()
const pageLoading = ref(false)
const saveLoading = ref(false)
const submitLoading = ref(false)

const selectedStoreId = ref(null)
const reportDate = ref(new Date().toISOString().slice(0, 10))
const selectedTemplateId = ref(null)
const storeOptions = ref([])
const templates = ref([])
const fields = ref([])
const reportId = ref(null)
const formData = reactive({})
const formRules = reactive({})

// 渠道字段列表
const channelFields = ['xiecheng', 'tongcheng', 'qunaer', 'zhixing', 'waiwang',
  'meituan_hotel', 'feizhu', 'douyin', 'xiaozhu', 'tujia', 'meituan_bnb', 'jiali']

// 是否可以选择门店（店长只能选自己门店）
const canSelectStore = computed(() => userStore.userInfo?.userType !== 2)

// 计算字段
const computedValues = computed(() => {
  const vals = {}
  const d = formData

  // 总营收 = 日租房房费 + 钟点房费 + 杂费
  const roomFee = Number(d.daily_room_fee || 0)
  const hourFee = Number(d.hourly_room_fee || 0)
  const miscFee = Number(d.other_fee || 0)
  vals.total_revenue = (roomFee + hourFee + miscFee).toFixed(2)

  // 出租率 = 间夜数 / 自有房量
  const rooms = Number(d.room_nights || 0)
  const ownRooms = Number(d.own_room_count || 1)
  vals.occupancy_rate = ownRooms > 0 ? (rooms / ownRooms * 100).toFixed(2) : '0.00'

  // ADR = 日租房房费 / 间夜数
  vals.adr = rooms > 0 ? (roomFee / rooms).toFixed(2) : '0.00'

  // RevPAR = 总营收 / 自有房量
  vals.revpar = ownRooms > 0 ? (Number(vals.total_revenue) / ownRooms).toFixed(2) : '0.00'

  return vals
})

// 分组字段
const groupedFields = computed(() => {
  const groups = {}
  for (const f of fields.value) {
    const gname = f.groupName || '其他字段'
    if (!groups[gname]) groups[gname] = { name: gname, fields: [] }
    groups[gname].fields.push(f)
  }
  return Object.values(groups)
})

function handleFieldChange(fieldName) {
  // 触发计算属性的重新渲染
  formData._touch = Date.now()
}

async function loadStoreOptions() {
  const res = await getStoreOptions()
  storeOptions.value = res.data || []

  if (userStore.userInfo?.userType === 2 && userStore.userInfo.storeIds?.length > 0) {
    selectedStoreId.value = userStore.userInfo.storeIds[0]
    loadTodayReport()
  }
}

async function loadTemplates() {
  try {
    const res = await request({ url: '/templates', method: 'get', params: { size: 999 } })
    templates.value = res.data?.records || []
    if (templates.value.length === 1) {
      selectedTemplateId.value = templates.value[0].id
      loadTemplateFields()
    }
  } catch (e) { /* ignore */ }
}

async function loadTemplateFields() {
  if (!selectedTemplateId.value) return
  pageLoading.value = true
  try {
    const res = await getTemplateFields(selectedTemplateId.value)
    fields.value = res.data || []
    initFormData()
  } catch (e) { /* ignore */ }
  finally { pageLoading.value = false }
}

function initFormData() {
  // 清空旧数据
  Object.keys(formData).forEach(k => delete formData[k])
  Object.keys(formRules).forEach(k => delete formRules[k])

  for (const f of fields.value) {
    if (f.readonlyFlag !== 1 && f.readonly !== 1) {
      formData[f.fieldName] = f.defaultValue ?? (f.fieldType === 'number' || f.fieldType === 1 ? 0 : '')
    }
    if (f.required === 1 && f.readonlyFlag !== 1 && f.readonly !== 1) {
      formRules[f.fieldName] = [
        { required: true, message: `请输入${f.label}`, trigger: 'blur' }
      ]
    }
  }

  formData._touch = Date.now()
}

async function loadTodayReport() {
  if (!selectedStoreId.value || !reportDate.value) return
  pageLoading.value = true
  try {
    const res = await getToday({ storeId: selectedStoreId.value, reportDate: reportDate.value })
    if (res.data) {
      // 后端返回 { report: {...}, fields: [...], values: {...} }
      const reportData = res.data.report || res.data
      reportId.value = reportData.id

      // 加载模板字段
      if (res.data.fields) {
        fields.value = res.data.fields
      }

      // 用 values 回填表单
      if (res.data.values) {
        initFormData()
        Object.keys(res.data.values).forEach(k => {
          if (formData.hasOwnProperty(k)) {
            formData[k] = res.data.values[k]
          }
        })
        formData._touch = Date.now()
      } else if (reportData.values) {
        initFormData()
        Object.keys(reportData.values).forEach(k => {
          if (formData.hasOwnProperty(k)) {
            formData[k] = reportData.values[k]
          }
        })
        formData._touch = Date.now()
      }
    } else {
      reportId.value = null
      if (fields.value.length > 0) {
        initFormData()
      } else {
        loadTemplateFields().then(() => {
          initFormData()
        })
      }
    }
  } catch (e) { /* ignore */ }
  finally { pageLoading.value = false }
}

async function handleSaveDraft() {
  if (!selectedStoreId.value) {
    ElMessage.warning('请选择门店')
    return
  }
  saveLoading.value = true
  try {
    const data = {
      reportId: reportId.value,
      storeId: selectedStoreId.value,
      reportDate: reportDate.value,
      templateId: selectedTemplateId.value,
      ...formData,
      ...computedValues.value
    }
    const res = await saveDraft(data)
    if (res.data?.id) reportId.value = res.data.id
    ElMessage.success('保存成功')
  } catch (e) { /* ignore */ }
  finally { saveLoading.value = false }
}

async function handleSubmit() {
  if (!selectedStoreId.value) {
    ElMessage.warning('请选择门店')
    return
  }

  // 校验必填字段
  const requiredFields = fields.value.filter(f => f.required === 1 && f.readonlyFlag !== 1 && f.readonly !== 1)
  for (const f of requiredFields) {
    const val = formData[f.fieldName]
    if (val === '' || val == null || val === undefined) {
      ElMessage.warning(`请填写${f.label}`)
      return
    }
  }

  await ElMessageBox.confirm('确认提交日报？提交后不可修改。', '确认提交', {
    confirmButtonText: '确认提交',
    type: 'warning'
  })

  submitLoading.value = true
  try {
    const data = {
      reportId: reportId.value,
      storeId: selectedStoreId.value,
      reportDate: reportDate.value,
      templateId: selectedTemplateId.value,
      ...formData,
      ...computedValues.value
    }
    await submit(data)
    ElMessage.success('提交成功')
    reportId.value = null
    loadTodayReport()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

onMounted(() => {
  loadStoreOptions()
  loadTemplates()
})
</script>

<style scoped>
.report-fill {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.top-card {
  margin-bottom: 0;
}

.action-col {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.template-card {
  margin-bottom: 0;
}

.field-group-card {
  margin-bottom: 0;
}

.group-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

@media (max-width: 768px) {
  .action-col {
    justify-content: flex-start;
    margin-top: 12px;
  }
}
</style>
