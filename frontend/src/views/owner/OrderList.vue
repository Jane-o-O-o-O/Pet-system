<template>
  <div class="page-shell order-list-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Owner Orders</span>
        <h1 class="page-head__title">寄养订单</h1>
        <p class="page-head__desc">提交寄养申请、查看订单进度，并按最新房费梯度自动计算价格。</p>
      </div>
      <div class="page-head__actions">
        <el-button type="primary" @click="handleAdd">申请寄养</el-button>
      </div>
    </section>

    <section class="page-panel">
      <div class="panel-topline">
        <span class="panel-topline__label">当前共 {{ total || orders.length }} 条寄养订单</span>
        <span class="panel-topline__label">房费梯度已调整为标准间 30、豪华间 40、套房 50</span>
      </div>

      <el-table :data="orders" v-loading="loading" class="order-table" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="petName" label="宠物" width="120" />
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
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              v-if="scope.row.status !== 'CANCELLED' && scope.row.status !== 'COMPLETED'"
              link
              type="danger"
              @click="handleCancel(scope.row.id)"
            >
              取消订单
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

    <el-dialog v-model="dialogVisible" title="申请寄养" width="520px">
      <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-width="100px">
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="orderForm.petId" placeholder="请选择宠物" style="width: 100%">
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
          <el-select v-model="orderForm.roomType" style="width: 100%">
            <el-option label="标准间 - ￥30/天" value="Standard" />
            <el-option label="豪华间 - ￥40/天" value="Deluxe" />
            <el-option label="套房 - ￥50/天" value="Suite" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估总价">
          <span class="price-value">￥{{ estimatedPrice }}</span>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../../utils/request'
import { daysBetweenDates, formatDate } from '../../utils/date'
import { roomPriceMap, roomTypeLabel } from '../../utils/business'

const route = useRoute()

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

const estimatedPrice = computed(() => {
  if (orderForm.dateRange.length !== 2) return 0
  const [startDate, endDate] = orderForm.dateRange
  if (!startDate || !endDate) return 0
  const roomPrice = roomPriceMap[orderForm.roomType] ?? roomPriceMap.Standard ?? 0
  return daysBetweenDates(startDate, endDate) * roomPrice
})

const fetchOrders = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/boarding-orders', { params: { page: page.value, size: pageSize.value } })
    orders.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const fetchPets = async () => {
  const res: any = await request.get('/pets', { params: { size: 999 } })
  myPets.value = res.data?.list || []
}

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
  fetchOrders()
}

const handleAdd = () => {
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
    ElMessage.success('寄养申请已提交')
    dialogVisible.value = false
    fetchOrders()
  } finally {
    saving.value = false
  }
}

const handleCancel = (id: number) => {
  ElMessageBox.confirm('确认取消这条寄养订单吗？', '提示', { type: 'warning' }).then(async () => {
    await request.put(`/boarding-orders/${id}/status`, { status: 'CANCELLED' })
    ElMessage.success('订单已取消')
    fetchOrders()
  }).catch(() => {})
}

onMounted(async () => {
  await Promise.all([fetchOrders(), fetchPets()])
  if (route.query.action === 'add') {
    handleAdd()
  }
})

watch(() => route.query.action, action => {
  if (action === 'add') {
    handleAdd()
  }
})
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

.price-value {
  font-size: 18px;
  color: #e6a23c;
  font-weight: bold;
}
</style>
