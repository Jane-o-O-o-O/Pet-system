<template>
  <div class="stats">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>用户总数</template>
          <div class="stat-value">{{ overview.users ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>宠物总数</template>
          <div class="stat-value">{{ overview.pets ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>医疗记录</template>
          <div class="stat-value">{{ overview.medicalRecords ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>寄养订单</template>
          <div class="stat-value">{{ overview.orders ?? '-' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>待确认订单</template>
          <div class="stat-value">{{ overview.createdOrders ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>处理中订单</template>
          <div class="stat-value">{{ overview.processingOrders ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>已完成订单</template>
          <div class="stat-value">{{ overview.completedOrders ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>已取消订单</template>
          <div class="stat-value">{{ overview.cancelledOrders ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>寄养统计</span>
          <div class="card-actions">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="fetchBoardingStats"
            />
            <el-button type="primary" plain @click="exportStatsReport">导出报表</el-button>
          </div>
        </div>
      </template>
      <div v-loading="boardingLoading">
        <div v-if="boardingStats">
          <p>订单数量: {{ boardingStats.count }}</p>
          <p>总收入: ¥{{ boardingStats.totalRevenue }}</p>
          <p>处理中订单: {{ boardingStats.processingCount ?? 0 }}</p>
          <p>已完成订单: {{ boardingStats.completedCount ?? 0 }}（完成率: {{ boardingStats.completionRate ?? 0 }}%）</p>
          <p>已取消订单: {{ boardingStats.cancelledCount ?? 0 }}（取消率: {{ boardingStats.cancellationRate ?? 0 }}%）</p>
          <div class="status-tags">
            <el-tag type="info">待确认 {{ boardingStats.statusCounts?.CREATED ?? 0 }}</el-tag>
            <el-tag>已确认 {{ boardingStats.statusCounts?.CONFIRMED ?? 0 }}</el-tag>
            <el-tag type="warning">寄养中 {{ boardingStats.statusCounts?.BOARDING ?? 0 }}</el-tag>
            <el-tag type="success">已完成 {{ boardingStats.statusCounts?.COMPLETED ?? 0 }}</el-tag>
            <el-tag type="danger">已取消 {{ boardingStats.statusCounts?.CANCELLED ?? 0 }}</el-tag>
          </div>

          <el-table
            :data="boardingStats.orders || []"
            style="width: 100%; margin-top: 16px;"
            max-height="360"
          >
            <el-table-column prop="orderNo" label="订单号" min-width="220" />
            <el-table-column prop="petName" label="宠物名称" width="120" />
            <el-table-column prop="ownerName" label="主人姓名" width="120" />
            <el-table-column prop="startDate" label="开始日期" width="120" />
            <el-table-column prop="endDate" label="结束日期" width="120" />
            <el-table-column label="房型" width="110">
              <template #default="scope">{{ roomTypeLabel(scope.row.roomType) }}</template>
            </el-table-column>
            <el-table-column label="总价" width="120">
              <template #default="scope">¥{{ scope.row.priceTotal ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">{{ statusLabel(scope.row.status) }}</template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else style="color: #999;">请选择日期范围查询</div>
      </div>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>疫苗到期提醒 (未来30天)</span>
          <el-tag type="danger">{{ overview.vaccineDueIn30Days ?? 0 }} 个到期</el-tag>
        </div>
      </template>
      <el-table :data="dueVaccines" v-loading="vaccineLoading" style="width: 100%">
        <el-table-column prop="petId" label="宠物ID" width="100" />
        <el-table-column prop="vaccineName" label="疫苗名称" />
        <el-table-column prop="nextDueDate" label="到期日期" width="150" />
        <el-table-column label="提醒状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.remindStatus === 'SENT' ? 'success' : 'warning'">
              {{ scope.row.remindStatus === 'SENT' ? '已提醒' : '待提醒' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const overview = ref<any>({})
const boardingStats = ref<any>(null)
const dueVaccines = ref<any[]>([])
const dateRange = ref<string[]>([])
const boardingLoading = ref(false)
const vaccineLoading = ref(false)

const statusLabel = (status: string) => ({
  CREATED: '待确认',
  CONFIRMED: '已确认',
  BOARDING: '寄养中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}[status] || status)

const roomTypeLabel = (roomType: string) => ({
  Standard: '标准间',
  Deluxe: '豪华间',
  Suite: '套房'
}[roomType] || roomType)

const fetchOverview = async () => {
  try {
    const res: any = await request.get('/admin/stats/overview')
    overview.value = res.data || {}
  } catch (error) {
    // handled by interceptor
  }
}

const fetchBoardingStats = async () => {
  if (dateRange.value && dateRange.value.length === 2) {
    boardingLoading.value = true
    try {
      const res: any = await request.get('/admin/stats/boarding', {
        params: { from: dateRange.value[0], to: dateRange.value[1] }
      })
      boardingStats.value = res.data || null
    } catch (error) {
      // handled by interceptor
    } finally {
      boardingLoading.value = false
    }
  }
}

const fetchDueVaccines = async () => {
  vaccineLoading.value = true
  try {
    const res: any = await request.get('/admin/stats/vaccine-due', { params: { days: 30 } })
    dueVaccines.value = res.data?.list || []
  } catch (error) {
    // handled by interceptor
  } finally {
    vaccineLoading.value = false
  }
}

const toCsvValue = (value: unknown): string => {
  const text = String(value ?? '')
  const escaped = text.replace(/"/g, '""')
  return `"${escaped}"`
}

const exportStatsReport = () => {
  if (!boardingStats.value || !dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请先选择日期范围并查询寄养统计后再导出')
    return
  }

  const rows: Array<Array<string | number>> = [
    ['统计项', '数值'],
    ['用户总数', overview.value.users ?? 0],
    ['宠物总数', overview.value.pets ?? 0],
    ['医疗记录', overview.value.medicalRecords ?? 0],
    ['寄养订单总数', overview.value.orders ?? 0],
    ['待确认订单（全量）', overview.value.createdOrders ?? 0],
    ['处理中订单（全量）', overview.value.processingOrders ?? 0],
    ['已完成订单（全量）', overview.value.completedOrders ?? 0],
    ['已取消订单（全量）', overview.value.cancelledOrders ?? 0],
    ['统计区间', `${dateRange.value[0]} 至 ${dateRange.value[1]}`],
    ['区间订单数量', boardingStats.value.count ?? 0],
    ['区间总收入', boardingStats.value.totalRevenue ?? 0],
    ['区间待确认', boardingStats.value.statusCounts?.CREATED ?? 0],
    ['区间已确认', boardingStats.value.statusCounts?.CONFIRMED ?? 0],
    ['区间寄养中', boardingStats.value.statusCounts?.BOARDING ?? 0],
    ['区间处理中', boardingStats.value.processingCount ?? 0],
    ['区间已完成', boardingStats.value.completedCount ?? 0],
    ['区间完成率(%)', boardingStats.value.completionRate ?? 0],
    ['区间已取消', boardingStats.value.cancelledCount ?? 0],
    ['区间取消率(%)', boardingStats.value.cancellationRate ?? 0],
    ['未来30天疫苗到期数', overview.value.vaccineDueIn30Days ?? 0]
  ]

  rows.push([])
  rows.push(['订单明细'])
  rows.push(['订单号', '宠物名称', '主人姓名', '开始日期', '结束日期', '房型', '总价', '状态'])

  const detailOrders = boardingStats.value.orders || []
  detailOrders.forEach((order: any) => {
    rows.push([
      order.orderNo ?? '',
      order.petName ?? '',
      order.ownerName ?? '',
      order.startDate ?? '',
      order.endDate ?? '',
      roomTypeLabel(order.roomType),
      order.priceTotal ?? 0,
      statusLabel(order.status)
    ])
  })

  const csv = rows.map(row => row.map(toCsvValue).join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const date = new Date().toISOString().slice(0, 10)
  link.href = url
  link.download = `统计报表_${date}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(() => {
  fetchOverview()
  fetchDueVaccines()
})
</script>

<style scoped>
.stat-value {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  color: #409eff;
}
.card-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.status-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
</style>
