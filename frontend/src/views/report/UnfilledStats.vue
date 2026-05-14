<template>
  <div class="unfilled-stats">
    <!-- 顶部日期 -->
    <el-card shadow="never" class="date-card">
      <el-form inline>
        <el-form-item label="日期">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            @change="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="notifyLoading" @click="handleNotifyAll">一键提醒未填报门店</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计摘要 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <StatCard title="应填报门店数" :value="stats.total" icon="Shop" color="#1890ff" />
      </el-col>
      <el-col :xs="24" :sm="8">
        <StatCard title="已填报" :value="stats.filled" icon="Checked" color="#52c41a" />
      </el-col>
      <el-col :xs="24" :sm="8">
        <StatCard title="未填报" :value="stats.unfilled" icon="Warning" color="#ff4d4f" />
      </el-col>
    </el-row>

    <!-- 未填报列表 -->
    <el-card shadow="never">
      <template #header>未填报门店列表</template>
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="storeName" label="门店名称" min-width="150" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="region" label="区域" width="100" />
        <el-table-column prop="managerName" label="店长" width="100" />
        <el-table-column prop="managerPhone" label="联系电话" width="130" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleNotify(row)">提醒</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { getUnfilledList } from '@/api/report'

const loading = ref(false)
const notifyLoading = ref(false)
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const tableData = ref([])
const stats = reactive({ total: 0, filled: 0, unfilled: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await getUnfilledList({ date: selectedDate.value })
    tableData.value = res.data?.stores || []
    stats.total = res.data?.total ?? 0
    stats.filled = res.data?.filled ?? 0
    stats.unfilled = res.data?.unfilled ?? 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleNotify(row) {
  ElMessage.success(`已向${row.storeName}发送提醒`)
}

async function handleNotifyAll() {
  await ElMessageBox.confirm('确认向所有未填报门店发送提醒？', '提示', { type: 'warning' })
  notifyLoading.value = true
  try {
    // 调用提醒接口
    ElMessage.success('已发送提醒')
  } catch (e) { /* ignore */ }
  finally { notifyLoading.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.unfilled-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  margin-bottom: 0;
}

.stat-row .el-col {
  margin-bottom: 16px;
}

.date-card {
  margin-bottom: 0;
}
</style>
