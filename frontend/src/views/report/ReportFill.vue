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

    <!-- 动态表单（四列紧凑 + 可折叠） -->
    <div v-if="fields.length > 0">
      <el-card
        v-for="group in groupedFields"
        :key="group.name"
        shadow="never"
        class="field-group-card compact-card"
      >
        <template #header>
          <div class="card-header" @click="toggleGroup(group.name)">
            <span class="group-title">{{ group.name }}</span>
            <el-icon class="collapse-icon" :class="{ 'is-collapsed': collapsedGroups[group.name] }">
              <ArrowDown />
            </el-icon>
          </div>
        </template>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-show="!collapsedGroups[group.name]">
          <el-row :gutter="12">
            <el-col
              v-for="field in group.fields"
              :key="field.fieldName"
              :xs="24"
              :sm="12"
              :md="6"
            >
              <el-form-item
                :label="field.label"
                :prop="field.fieldName"
                :required="field.required === 1 && field.readonlyFlag !== 1 && field.readonly !== 1"
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
import { ArrowDown } from '@element-plus/icons-vue'
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

// 分组折叠状态
const collapsedGroups = reactive({})
function toggleGroup(name) {
  collapsedGroups[name] = !collapsedGroups[name]
}

// 渠道间夜字段编码（与数据库 daily_report_field 一致）
const CHANNEL_ROOM_NIGHTS_KEYS = [
  'walkin_room_nights',              // 散客
  'ctrip_room_nights',               // 携程
  'ly_room_nights',                  // 同程艺龙
  'qunar_room_nights',               // 去哪儿
  'zhixing_room_nights',             // 智行
  'external_room_nights',            // 外网
  'meituan_hotel_room_nights',       // 美团酒店
  'fliggy_room_nights',              // 飞猪
  'douyin_room_nights',              // 抖音
  'xiaozhu_room_nights',             // 小猪
  'tujia_room_nights',               // 途家
  'meituan_homestay_room_nights',    // 美团民宿
  'jiali_room_nights',               // 红色加力/加力
]

// 是否可以选择门店（店长只能选自己门店）
const canSelectStore = computed(() => userStore.userInfo?.userType !== 2)

// 计算字段
const computedValues = computed(() => {
  const vals = {}
  const d = formData

  // 间夜数 = 各渠道房间数之和 + 钟点房数量
  const channelSum = CHANNEL_ROOM_NIGHTS_KEYS.reduce((sum, key) => sum + Number(d[key] || 0), 0)
  const hourlyRoomCount = Number(d.hourly_room_count || 0)
  vals.room_nights = channelSum + hourlyRoomCount

  const rooms = vals.room_nights
  const ownRooms = Number(d.own_room_count || 0)

  // 出租率 = 间夜数 / 自有房量 * 100
  vals.occupancy_rate = ownRooms > 0 ? (rooms / ownRooms * 100).toFixed(2) : '0.00'

  // 当日总房费 = 日租房房费 + 钟点房费
  const roomFee = Number(d.daily_room_fee || 0)
  const hourFee = Number(d.hourly_room_fee || 0)
  const miscFee = Number(d.other_fee || 0)
  const totalRoomFee = roomFee + hourFee

  // ADR = 当日总房费 / 间夜数
  vals.adr = rooms > 0 ? (totalRoomFee / rooms).toFixed(2) : '0.00'

  // 当日总营收 = 当日总房费 + 杂费
  vals.total_revenue = (totalRoomFee + miscFee).toFixed(2)

  // RevPAR = 当日总营收 / 自有房量
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
  gap: 10px;
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

.compact-card :deep(.el-card__header) {
  padding: 8px 12px;
}

.compact-card :deep(.el-card__body) {
  padding: 8px 12px;
}

.compact-card :deep(.el-form-item) {
  margin-bottom: 4px;
}

.compact-card :deep(.el-form-item__label) {
  font-size: 13px;
  padding-right: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.card-header:hover {
  background: #f5f7fa;
  margin: -8px -12px;
  padding: 8px 12px;
  border-radius: 4px 4px 0 0;
}

.collapse-icon {
  transition: transform 0.3s;
  color: #909399;
}

.collapse-icon.is-collapsed {
  transform: rotate(-90deg);
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

@media (max-width: 768px) {
  .action-col {
    justify-content: flex-start;
    margin-top: 8px;
  }
}
</style>
