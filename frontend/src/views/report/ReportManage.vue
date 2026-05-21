<template>
  <div class="report-manage">
    <!-- 搜索区域 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="门店">
          <el-select v-model="searchForm.storeId" placeholder="全部" clearable style="width:200px">
            <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width:260px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="草稿" :value="0" />
            <el-option label="已提交" :value="1" />
            <el-option label="已锁定" :value="2" />
            <el-option label="已退回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card shadow="never">
      <div class="toolbar">
        <span></span>
        <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新增日报</el-button>
      </div>
      <el-table :data="tableData" border stripe v-loading="listLoading" style="width:100%">
        <el-table-column prop="storeName" label="门店" min-width="130" />
        <el-table-column prop="reportDate" label="日期" width="110" />
        <el-table-column prop="roomNights" label="间夜数" width="80" align="right" />
        <el-table-column label="出租率" width="80" align="right">
          <template #default="{ row }">{{ row.occupancyRate }}%</template>
        </el-table-column>
        <el-table-column label="ADR" width="90" align="right">
          <template #default="{ row }">¥{{ row.adr?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="总营收" width="120" align="right">
          <template #default="{ row }">¥{{ row.totalRevenue?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
            <!-- 草稿或已退回均可编辑 -->
            <el-button v-if="row.status === 0 || row.status === 3" type="success" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" link @click="handleLock(row)">锁定</el-button>
            <el-button v-if="row.status === 2" type="warning" size="small" link @click="handleUnlock(row)">解锁</el-button>
            <el-button v-if="row.status === 1" type="danger" size="small" link @click="handleReject(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 查看详情弹窗（只读，紧凑展示） -->
    <el-dialog v-model="detailVisible" title="日报详情" width="900px" close-on-click-modal="false">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailData" :column="4" border class="detail-base-info" size="small">
          <el-descriptions-item label="门店">{{ detailData.storeName }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ detailData.reportDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)" size="small">{{ statusLabel(detailData.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 分组展示字段数据（可折叠） -->
        <div v-if="detailFields.length > 0" class="detail-fields-area">
          <el-card
            v-for="group in detailGroupedFields"
            :key="group.name"
            shadow="never"
            class="detail-card compact-card"
          >
            <template #header>
              <div class="card-header" @click="toggleDetailGroup(group.name)">
                <span class="group-title">{{ group.name }}</span>
                <el-icon class="collapse-icon" :class="{ 'is-collapsed': detailCollapsedGroups[group.name] }">
                  <ArrowDown />
                </el-icon>
              </div>
            </template>
            <el-descriptions :column="4" border size="small" v-show="!detailCollapsedGroups[group.name]">
              <el-descriptions-item
                v-for="field in group.fields"
                :key="fieldKey(field)"
                :label="fieldLabel(field)"
              >
                {{ formatFieldValue(field, detailValues[fieldKey(field)]) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
        <el-empty v-else-if="!detailLoading" description="暂无字段数据" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退回弹窗 -->
    <el-dialog v-model="rejectVisible" title="退回日报" width="450px" close-on-click-modal="false">
      <el-form>
        <el-form-item label="退回原因">
          <el-input v-model="rejectReason" type="textarea" rows="3" placeholder="请输入退回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRejectSubmit">确认退回</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑 抽屉（宽度加大，三列布局） -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="1100px"
      :close-on-click-modal="false"
      @closed="resetDrawer"
    >
      <div class="drawer-content" v-loading="drawerLoading">
        <!-- 基础信息 -->
        <el-card shadow="never" class="drawer-card">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="门店" label-width="80px" required>
                <el-select
                  v-model="selectedStoreId"
                  placeholder="请选择门店"
                  @change="onStoreOrDateChange"
                  :disabled="drawerMode === 'edit'"
                  style="width:100%"
                >
                  <el-option v-for="s in storeOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="日期" label-width="80px" required>
                <el-date-picker
                  v-model="reportDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                  @change="onStoreOrDateChange"
                  :disabled="drawerMode === 'edit'"
                  style="width:100%"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="templates.length > 1" :span="8">
              <el-form-item label="模板" label-width="80px">
                <el-select v-model="selectedTemplateId" @change="loadTemplateFields" style="width:100%">
                  <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <!-- 已退回提示 -->
            <el-col v-if="isReturnedEdit" :span="24">
              <el-alert type="warning" :closable="false" style="margin-top:8px">
                <template #title>该日报已退回，修改后可重新保存草稿或提交</template>
              </el-alert>
            </el-col>
          </el-row>
        </el-card>

        <!-- 动态字段表单（四列紧凑） -->
        <div v-if="fields.length > 0" class="fields-area">
          <el-card
            v-for="group in groupedFields"
            :key="group.name"
            shadow="never"
            class="drawer-card compact-card"
          >
            <template #header>
              <div class="card-header" @click="toggleFormGroup(group.name)">
                <span class="group-title">{{ group.name }}</span>
                <el-icon class="collapse-icon" :class="{ 'is-collapsed': formCollapsedGroups[group.name] }">
                  <ArrowDown />
                </el-icon>
              </div>
            </template>
            <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-show="!formCollapsedGroups[group.name]">
              <el-row :gutter="12">
                <el-col
                  v-for="field in group.fields"
                  :key="fieldKey(field)"
                  :xs="24"
                  :sm="12"
                  :md="6"
                >
                  <el-form-item
                    :label="fieldLabel(field)"
                    :prop="fieldKey(field)"
                    :required="field.required === 1 && field.readonlyFlag !== 1 && field.readonly !== 1"
                  >
                    <!-- 只读/计算字段 -->
                    <el-input
                      v-if="field.readonlyFlag === 1 || field.readonly === 1"
                      :model-value="computedValues[fieldKey(field)] ?? '-'"
                      disabled
                      style="background:#f5f5f5;width:100%"
                    />
                    <!-- 数字类型 -->
                    <el-input-number
                      v-else-if="field.fieldType === 'number' || field.fieldType === 1"
                      v-model="formData[fieldKey(field)]"
                      :min="0"
                      :precision="getFieldPrecision(field)"
                      :step="getFieldStep(field)"
                      controls-position="right"
                      style="width:100%"
                      @change="handleFieldChange"
                    />
                    <!-- 文本类型 -->
                    <el-input
                      v-else
                      v-model="formData[fieldKey(field)]"
                      :placeholder="'请输入' + fieldLabel(field)"
                      style="width:100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>
        </div>
        <el-empty v-else-if="!drawerLoading && selectedStoreId && reportDate" description="暂无表单字段，请检查模板配置" />
        <el-empty v-else description="请选择门店和日期以加载表单" />
      </div>

      <!-- 底部按钮 -->
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDraft" :loading="saveLoading">保存草稿</el-button>
        <el-button type="success" @click="handleSubmit" :loading="submitLoading">提交</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, ArrowDown } from '@element-plus/icons-vue'
import { getStoreOptions } from '@/api/store'
import { getToday, getDetail, saveDraft, submit, queryList, lock, unlock, reject, getTemplateFields } from '@/api/report'
import request from '@/utils/request'

// ===== 工具函数 =====
function statusLabel(status) {
  const map = { 0: '草稿', 1: '已提交', 2: '已锁定', 3: '已退回' }
  return map[status] ?? '未知'
}
function statusTagType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return map[status] ?? 'info'
}

// ===== 列表相关 =====
const listLoading = ref(false)
const searchForm = reactive({ storeId: null, dateRange: null, status: null })
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref([])
const storeOptions = ref([]) // [{label, value}] 格式

async function loadStoreOptions() {
  try {
    const res = await getStoreOptions()
    storeOptions.value = res.data || []
  } catch (e) {
    console.error('加载门店选项失败', e)
    storeOptions.value = []
  }
}

async function loadList() {
  listLoading.value = true
  try {
    const params = { page: page.value, size: size.value, ...searchForm }
    if (params.dateRange) {
      params.startDate = params.dateRange[0]
      params.endDate = params.dateRange[1]
      delete params.dateRange
    }
    const res = await queryList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  finally { listLoading.value = false }
}

function handleSearch() { page.value = 1; loadList() }
function handleReset() {
  searchForm.storeId = null; searchForm.dateRange = null; searchForm.status = null
  page.value = 1; loadList()
}

// ===== 查看详情弹窗（只读，完整字段展示） =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)
const detailFields = ref([])
const detailValues = ref({})

const detailGroupedFields = computed(() => {
  const groups = {}
  for (const f of detailFields.value) {
    const gname = fieldGroup(f)
    if (!groups[gname]) groups[gname] = { name: gname, fields: [] }
    groups[gname].fields.push(f)
  }
  return Object.values(groups)
})

function formatFieldValue(field, val) {
  if (val === null || val === undefined || val === '') return '-'
  const prec = getFieldPrecision(field)
  if ((field.fieldType === 'number' || field.fieldType === 1) && !isNaN(val)) {
    return Number(val).toFixed(prec)
  }
  return val
}

async function handleView(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  detailFields.value = []
  detailValues.value = {}
  try {
    const res = await getDetail({ storeId: row.storeId, reportDate: row.reportDate })
    const data = res.data || {}
    detailData.value = data.report || data
    detailFields.value = data.fields || []
    detailValues.value = data.values || {}
    // 补充计算字段值到 detailValues
    if (detailFields.value.length > 0) {
      const cv = calcComputedValues(detailValues.value)
      Object.assign(detailValues.value, cv)
    }
  } catch (e) {
    ElMessage.error('加载日报详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ===== 锁定/解锁/退回 =====
async function handleLock(row) {
  await ElMessageBox.confirm('锁定后将无法修改，确认锁定？', '提示', { type: 'warning' })
  await lock(row.id)
  row.status = 2
  ElMessage.success('已锁定')
}

async function handleUnlock(row) {
  await ElMessageBox.confirm('确认解锁该日报？', '提示', { type: 'warning' })
  await unlock(row.id)
  row.status = 1
  ElMessage.success('已解锁')
}

const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)

function handleReject(row) {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectSubmit() {
  if (!rejectReason.value) {
    ElMessage.warning('请输入退回原因')
    return
  }
  await reject(currentRow.value.id, rejectReason.value)
  currentRow.value.status = 3
  rejectVisible.value = false
  ElMessage.success('已退回')
}

// ===== 抽屉 - 填报/编辑表单 =====
const drawerVisible = ref(false)
const drawerMode = ref('add') // 'add' | 'edit'
const drawerLoading = ref(false)
const saveLoading = ref(false)
const submitLoading = ref(false)
const isReturnedEdit = ref(false) // 是否编辑已退回日报

const drawerTitle = computed(() => {
  if (drawerMode.value === 'add') return '新增日报'
  if (isReturnedEdit.value) return '编辑日报（已退回）'
  return '编辑日报'
})

const selectedStoreId = ref(null)
const reportDate = ref(null)
const selectedTemplateId = ref(null)
const templates = ref([])
const fields = ref([])
const reportId = ref(null)
const formData = reactive({})
const formRules = reactive({})

// ===== 分组折叠状态 =====
const formCollapsedGroups = reactive({})
const detailCollapsedGroups = reactive({})
function toggleFormGroup(name) {
  formCollapsedGroups[name] = !formCollapsedGroups[name]
}
function toggleDetailGroup(name) {
  detailCollapsedGroups[name] = !detailCollapsedGroups[name]
}

// ===== 字段精度判断 =====
// 房间数/间夜/扫码：整数（precision=0）
// 金额相关：两位小数（precision=2）
// 评分相关：一位小数（precision=1）
// 好评数：整数（precision=0）
const INTEGER_FIELD_KEYWORDS = ['room', 'nights', 'count', 'num', 'qr', 'scan', 'praise', 'good_review', 'praise_count', 'review_count']
const SCORE_FIELD_KEYWORDS = ['score', 'rate', 'rating', 'satisfaction', 'nps', 'star']
const AMOUNT_FIELD_KEYWORDS = ['fee', 'revenue', 'income', 'amount', 'price', 'adr', 'revpar', 'cost']

function getFieldPrecision(field) {
  const code = (field.fieldCode || field.fieldName || '').toLowerCase()
  // 计算/只读字段（出租率、ADR、RevPAR等）
  if (field.readonlyFlag === 1 || field.readonly === 1) {
    if (SCORE_FIELD_KEYWORDS.some(k => code.includes(k))) return 1
    return 2
  }
  // 整数字段
  if (INTEGER_FIELD_KEYWORDS.some(k => code.includes(k))) return 0
  // 评分字段
  if (SCORE_FIELD_KEYWORDS.some(k => code.includes(k))) return 1
  // 金额字段
  if (AMOUNT_FIELD_KEYWORDS.some(k => code.includes(k))) return 2
  // 默认两位小数
  return 2
}

function getFieldStep(field) {
  const prec = getFieldPrecision(field)
  if (prec === 0) return 1
  if (prec === 1) return 0.1
  return 0.01
}

// ===== 渠道间夜字段编码（与数据库 daily_report_field 一致） =====
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

// ===== 计算字段 =====
function calcComputedValues(d) {
  const vals = {}

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
}

const computedValues = computed(() => calcComputedValues(formData))

// ===== 字段工具函数 =====
function fieldKey(f) {
  return f.fieldCode || f.fieldName
}
function fieldLabel(f) {
  return f.fieldName || f.label || f.fieldCode
}
function fieldGroup(f) {
  return f.fieldCategory || f.groupName || '其他字段'
}

const groupedFields = computed(() => {
  const groups = {}
  for (const f of fields.value) {
    const gname = fieldGroup(f)
    if (!groups[gname]) groups[gname] = { name: gname, fields: [] }
    groups[gname].fields.push(f)
  }
  return Object.values(groups)
})

function handleFieldChange() {
  formData._touch = Date.now()
}

function initFormData() {
  Object.keys(formData).forEach(k => delete formData[k])
  Object.keys(formRules).forEach(k => delete formRules[k])

  for (const f of fields.value) {
    const key = fieldKey(f)
    if (f.readonlyFlag !== 1 && f.readonly !== 1) {
      const prec = getFieldPrecision(f)
      const defaultNum = prec === 0 ? 0 : 0
      formData[key] = f.defaultValue ?? (f.fieldType === 'number' || f.fieldType === 1 ? defaultNum : '')
    }
    if (f.required === 1 && f.readonlyFlag !== 1 && f.readonly !== 1) {
      formRules[key] = [
        { required: true, message: `请输入${fieldLabel(f)}`, trigger: 'blur' }
      ]
    }
  }
  formData._touch = Date.now()
}

async function loadTemplates() {
  try {
    const res = await request({ url: '/templates', method: 'get', params: { size: 999 } })
    templates.value = res.data?.records || []
    if (templates.value.length === 1) {
      selectedTemplateId.value = templates.value[0].id
    }
  } catch (e) { console.error('加载模板列表失败', e) }
}

async function loadTemplateFields() {
  if (!selectedTemplateId.value) return
  drawerLoading.value = true
  try {
    const res = await getTemplateFields(selectedTemplateId.value)
    fields.value = res.data || []
    initFormData()
  } catch (e) { console.error('加载模板字段失败', e) }
  finally { drawerLoading.value = false }
}

// 门店或日期变化时自动加载数据（新增/编辑均适用）
async function onStoreOrDateChange() {
  if (!selectedStoreId.value || !reportDate.value) {
    fields.value = []
    initFormData()
    return
  }

  drawerLoading.value = true
  try {
    const res = await getToday({ storeId: selectedStoreId.value, reportDate: reportDate.value })
    if (res.data) {
      const reportData = res.data.report || res.data
      // 新增模式：已提交/已锁定不可重复填报
      if (drawerMode.value === 'add' && (reportData.status === 1 || reportData.status === 2)) {
        ElMessage.warning('该门店当日已有已提交/已锁定的日报，不可重复填报')
        selectedStoreId.value = null
        fields.value = []
        initFormData()
        return
      }
      reportId.value = reportData.id

      if (res.data.fields) {
        fields.value = res.data.fields
      }

      if (res.data.values) {
        initFormData()
        Object.keys(res.data.values).forEach(k => {
          if (k in formData) {
            formData[k] = res.data.values[k]
          }
        })
        formData._touch = Date.now()
      } else {
        // 有报告但无values，可能是刚创建的草稿，加载字段
        if (!res.data.fields && templates.value.length > 0 && selectedTemplateId.value) {
          await loadTemplateFields()
        }
      }
    } else {
      reportId.value = null
      // 无已有数据，加载模板字段
      if (templates.value.length === 1) {
        selectedTemplateId.value = templates.value[0].id
        await loadTemplateFields()
      } else if (selectedTemplateId.value) {
        await loadTemplateFields()
      }
    }
  } catch (e) { console.error('加载门店日报数据失败', e) }
  finally { drawerLoading.value = false }
}

// ===== 新增日报 =====
async function handleAdd() {
  drawerMode.value = 'add'
  isReturnedEdit.value = false
  reportId.value = null
  selectedStoreId.value = null
  reportDate.value = null
  selectedTemplateId.value = null
  fields.value = []
  initFormData()
  drawerVisible.value = true
  // 先加载模板列表，再等用户选门店+日期触发字段加载
  await loadTemplates()
}

// ===== 编辑日报（草稿 / 已退回） =====
async function handleEdit(row) {
  drawerMode.value = 'edit'
  isReturnedEdit.value = row.status === 3
  selectedStoreId.value = row.storeId
  reportDate.value = row.reportDate
  reportId.value = row.id
  drawerVisible.value = true
  drawerLoading.value = true

  try {
    const res = await getDetail({ storeId: row.storeId, reportDate: row.reportDate })
    if (res.data) {
      const reportData = res.data.report || res.data
      reportId.value = reportData.id
      if (reportData.templateId) selectedTemplateId.value = reportData.templateId

      if (res.data.fields) {
        fields.value = res.data.fields
      }

      if (res.data.values) {
        initFormData()
        Object.keys(res.data.values).forEach(k => {
          if (k in formData) {
            formData[k] = res.data.values[k]
          }
        })
        formData._touch = Date.now()
      }
    }
  } catch (e) { console.error('加载日报详情失败', e) }
  finally { drawerLoading.value = false }
}

// ===== 保存草稿 =====
async function handleSaveDraft() {
  if (!selectedStoreId.value) {
    ElMessage.warning('请选择门店')
    return
  }
  if (!reportDate.value) {
    ElMessage.warning('请选择日期')
    return
  }
  saveLoading.value = true
  try {
    const data = buildSaveData()
    const res = await saveDraft(data)
    if (res.data?.id) reportId.value = res.data.id
    ElMessage.success('保存成功')
    // 已退回保存草稿后状态保持3（不改变列表状态，后端决定）
    loadList()
  } catch (e) { /* ignore */ }
  finally { saveLoading.value = false }
}

// ===== 提交日报 =====
async function handleSubmit() {
  if (!selectedStoreId.value) {
    ElMessage.warning('请选择门店')
    return
  }
  if (!reportDate.value) {
    ElMessage.warning('请选择日期')
    return
  }

  const requiredFields = fields.value.filter(f => f.required === 1 && f.readonlyFlag !== 1 && f.readonly !== 1)
  for (const f of requiredFields) {
    const val = formData[fieldKey(f)]
    if (val === '' || val == null || val === undefined) {
      ElMessage.warning(`请填写${fieldLabel(f)}`)
      return
    }
  }

  await ElMessageBox.confirm('确认提交日报？提交后状态变为已提交。', '确认提交', {
    confirmButtonText: '确认提交',
    type: 'warning'
  })

  submitLoading.value = true
  try {
    const data = buildSaveData()
    await submit(data)
    ElMessage.success('提交成功')
    drawerVisible.value = false
    loadList()
  } catch (e) { /* ignore */ }
  finally { submitLoading.value = false }
}

function buildSaveData() {
  return {
    reportId: reportId.value,
    storeId: selectedStoreId.value,
    reportDate: reportDate.value,
    templateId: selectedTemplateId.value,
    ...formData,
    ...computedValues.value
  }
}

// 关闭抽屉时重置
function resetDrawer() {
  reportId.value = null
  selectedStoreId.value = null
  reportDate.value = null
  selectedTemplateId.value = null
  isReturnedEdit.value = false
  fields.value = []
  initFormData()
}

onMounted(() => {
  loadStoreOptions()
  loadList()
})
</script>

<style scoped>
.report-manage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-card {
  margin-bottom: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.drawer-content {
  padding: 0 8px;
}

.drawer-card {
  margin-bottom: 6px;
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

.compact-card :deep(.el-descriptions__cell) {
  padding: 4px 8px !important;
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

.fields-area {
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding-right: 4px;
}

.detail-base-info {
  margin-bottom: 8px;
}

.detail-fields-area {
  margin-top: 8px;
}

.detail-card {
  margin-bottom: 6px;
}
</style>
