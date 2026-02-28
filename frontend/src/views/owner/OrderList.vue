<template>
  <div class="order-list">
    <div class="header">
      <el-button type="primary" @click="handleAdd">申请寄养</el-button>
    </div>

    <el-table :data="orders" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="petName" label="宠物" width="120" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="roomType" label="房型" width="100">
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
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button
            v-if="scope.row.status !== 'CANCELLED' && scope.row.status !== 'COMPLETED'"
            link type="danger" @click="handleCancel(scope.row.id)"
          >取消订单</el-button>
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

    <el-dialog v-model="dialogVisible" title="申请寄养" width="500px">
      <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-width="100px">
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="orderForm.petId" placeholder="请选择宠物">
            <el-option v-for="pet in myPets" :key="pet.id" :label="pet.name" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="寄养日期" prop="dateRange">
          <el-date-picker
            v-model="orderForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="房型" prop="roomType">
          <el-select v-model="orderForm.roomType">
            <el-option label="标准间 - ¥100/天" value="Standard" />
            <el-option label="豪华间 - ¥200/天" value="Deluxe" />
            <el-option label="套房 - ¥300/天" value="Suite" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估总价">
          <span style="font-size: 18px; color: #e6a23c; font-weight: bold;">¥{{ estimatedPrice }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveOrder">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

const orders = ref<any[]>([])
const myPets = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const orderFormRef = ref<FormInstance>()

const orderForm = reactive({
  petId: null as number | null,
  dateRange: [] as string[],
  roomType: 'Standard',
  remark: ''
})

const orderRules: FormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择寄养日期', trigger: 'change' }],
  roomType: [{ required: true, message: '请选择房型', trigger: 'change' }]
}

const roomPriceMap: Record<string, number> = { Standard: 100, Deluxe: 200, Suite: 300 }
const roomTypeLabel = (t: string) => ({ Standard: '标准间', Deluxe: '豪华间', Suite: '套房' }[t] || t)
const statusLabel = (s: string) => ({ CREATED: '待确认', CONFIRMED: '已确认', BOARDING: '寄养中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s)
const getStatusType = (s: string) => ({ CREATED: 'info', CONFIRMED: '', BOARDING: 'warning', COMPLETED: 'success', CANCELLED: 'danger' }[s] || '')

const estimatedPrice = computed(() => {
  if (orderForm.dateRange?.length === 2) {
    const days = Math.max(1, Math.ceil((new Date(orderForm.dateRange[1]).getTime() - new Date(orderForm.dateRange[0]).getTime()) / 86400000))
    return days * (roomPriceMap[orderForm.roomType] || 100)
  }
  return 0
})

const getPetName = (petId: number) => {
  const pet = myPets.value.find((p: any) => p.id === petId)
  return pet?.name || petId
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/boarding-orders', { params: { page: page.value, size: pageSize.value } })
    orders.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const fetchPets = async () => {
  try {
    const res: any = await request.get('/pets', { params: { size: 999 } })
    myPets.value = res.data?.list || []
  } catch (error) {
    // handled by interceptor
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchOrders()
}

const handleAdd = () => {
  fetchPets()
  Object.assign(orderForm, { petId: null, dateRange: [], roomType: 'Standard', remark: '' })
  dialogVisible.value = true
}

const saveOrder = async () => {
  const valid = await orderFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await request.post('/boarding-orders', {
      petId: orderForm.petId,
      startDate: orderForm.dateRange[0],
      endDate: orderForm.dateRange[1],
      roomType: orderForm.roomType,
      priceTotal: estimatedPrice.value,
      remark: orderForm.remark
    })
    ElMessage.success('申请成功')
    dialogVisible.value = false
    fetchOrders()
  } catch (error) {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

const handleCancel = (id: number) => {
  ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.put(`/boarding-orders/${id}/status`, { status: 'CANCELLED' })
      ElMessage.success('订单已取消')
      fetchOrders()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchOrders()
  fetchPets()
})
</script>

<style scoped>
.header { margin-bottom: 20px; }
</style>
