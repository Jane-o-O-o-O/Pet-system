<template>
  <div class="order-management">
    <div class="header">
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable @change="fetchOrders" style="width: 200px;">
        <el-option label="待确认" value="CREATED" />
        <el-option label="已确认" value="CONFIRMED" />
        <el-option label="寄养中" value="BOARDING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
    </div>

    <el-table :data="orders" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="petName" label="宠物名称" width="120" />
      <el-table-column prop="ownerName" label="主人姓名" width="120" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column label="房型" width="100">
        <template #default="scope">{{ roomTypeLabel(scope.row.roomType) }}</template>
      </el-table-column>
      <el-table-column label="总价" width="100">
        <template #default="scope">¥{{ scope.row.priceTotal || 0 }}</template>
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
            link type="danger" @click="updateStatus(scope.row.id, 'CANCELLED')"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      style="margin-top: 16px; justify-content: flex-end;"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref<any[]>([])
const statusFilter = ref('')
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusLabel = (s: string) => ({ CREATED: '待确认', CONFIRMED: '已确认', BOARDING: '寄养中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s)
const getStatusType = (s: string) => ({ CREATED: 'info', CONFIRMED: 'primary', BOARDING: 'warning', COMPLETED: 'success', CANCELLED: 'danger' }[s])
const roomTypeLabel = (t: string) => ({ Standard: '标准间', Deluxe: '豪华间', Suite: '套房' }[t] || t)

const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await request.get('/boarding-orders', { params })
    orders.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchOrders()
}

const updateStatus = async (id: number, status: string) => {
  const actionLabel = statusLabel(status)
  ElMessageBox.confirm(`确定将订单状态更新为"${actionLabel}"吗？`, '提示', { type: 'warning' }).then(async () => {
    try {
      await request.put(`/boarding-orders/${id}/status`, { status })
      ElMessage.success('状态更新成功')
      fetchOrders()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(fetchOrders)
</script>

<style scoped>
.header { margin-bottom: 20px; }
</style>
