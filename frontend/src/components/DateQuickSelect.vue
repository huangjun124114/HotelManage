<template>
  <el-button-group>
    <el-button size="small" @click="setRange('week')">本周</el-button>
    <el-button size="small" @click="setRange('month')">本月</el-button>
    <el-button size="small" @click="setRange('year')">本年</el-button>
  </el-button-group>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

function setRange(type) {
  const now = new Date()
  let start, end

  if (type === 'week') {
    // ISO周：周一至周日
    const day = now.getDay() // 0=周日, 1=周一...
    const diffToMon = day === 0 ? -6 : 1 - day // 距周一的天数（周日特殊处理）
    start = new Date(now)
    start.setDate(now.getDate() + diffToMon)
    end = new Date(start)
    end.setDate(start.getDate() + 6)
  } else if (type === 'month') {
    // 本月1日至最后一日
    start = new Date(now.getFullYear(), now.getMonth(), 1)
    end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  } else if (type === 'year') {
    // 本年1月1日至12月31日
    start = new Date(now.getFullYear(), 0, 1)
    end = new Date(now.getFullYear(), 11, 31)
  }

  const formatDate = (d) => d.toISOString().slice(0, 10)
  emit('update:modelValue', [formatDate(start), formatDate(end)])
}
</script>
