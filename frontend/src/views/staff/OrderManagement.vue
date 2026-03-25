<template>
  <div class="page-shell order-management-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Boarding Desk</span>
        <h1 class="page-head__title">寄养流程管理</h1>
        <p class="page-head__desc">医护和后台统一处理寄养确认、入住、完成与取消流程。</p>
      </div>
    </section>

    <section class="page-panel">
      <div class="page-toolbar">
        <el-select v-model="statusFilter" placeholder="按状态筛选" clearable @change="fetchOrders" style="width: 220px">
          <el-option label="待确认" value="CREATED" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="寄养中" value="BOARDING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <span class="panel-topline__label">当前共 {{ total || orders.length }} 条寄养订单</span>
      </div>

      <el-table :data="orders" v-loading="loading" class="order-table" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="petName" label="宠物名称" width="120" />
        <el-table-column prop="ownerName" label="主人姓名" width="120" />
        <el-table-column label="开始日期" width="120">
          <template #default="scope">{{ formatDate(scope.row.startDate) }}</template>
        </el-table-column>
        <el-table-column label="结束日期" width="120">
          <template #default="scope">{{ formatDate(scope.row.endDate) }}</template>
        </el-table-column>
        <el-table-column label="房型" width="100">
          <template #default="scope">{{ roomTypeLabel(scope.row.roomType) }}</template>
        </el-table-column>
        <el-table-column label="总价" width="100">
          <template #default="scope">￥{{ scope.row.priceTotal || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button v-if="scope.row.status === 'CREATED'" link type="primary" @click="updateStatus(scope.row.id, 'CONFIRMED')">确认订单</el-button>
            <el-button v-if="scope.row.status === 'CONFIRMED'" link type="warning" @click="updateStatus(scope.row.id, 'BOARDING')">开始寄养</el-button>
            <el-button v-if="scope.row.status === 'BOARDING'" link type="success" @click="updateStatus(scope.row.id, 'COMPLETED')">完成寄养</el-button>
            <el-button
              v-if="scope.row.status !== 'CANCELLED' && scope.row.status !== 'COMPLETED'"
              link
              type="danger"
              @click="updateStatus(scope.row.id, 'CANCELLED')"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { formatDate } from '../../utils/date'
import { roomTypeLabel } from '../../utils/business'

const orders = ref<any[]>([])
const statusFilter = ref('')
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusLabel = (status: string) => ({
  CREATED: '待确认',
  CONFIRMED: '已确认',
  BOARDING: '寄养中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}[status] || status)

const getStatusType = (status: string) => ({
  CREATED: 'info',
  CONFIRMED: 'primary',
  BOARDING: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}[status] || '')

const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await request.get('/boarding-orders', { params })
    orders.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
  fetchOrders()
}

const updateStatus = async (id: number, status: string) => {
  ElMessageBox.confirm(`确认将订单状态更新为“${statusLabel(status)}”吗？`, '提示', { type: 'warning' }).then(async () => {
    await request.put(`/boarding-orders/${id}/status`, { status })
    ElMessage.success('订单状态已更新')
    fetchOrders()
  }).catch(() => {})
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-table {
  width: 100%;
}

.order-table:deep(.el-table__header),
.order-table:deep(.el-table__body),
.order-table:deep(.el-table__footer) {
  width: 100% !important;
}

.order-table:deep(.el-table__header-wrapper table),
.order-table:deep(.el-table__body-wrapper table) {
  width: 100% !important;
}
</style>
