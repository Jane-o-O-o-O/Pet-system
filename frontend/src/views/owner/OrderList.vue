<template>
  <div class="page-shell order-list-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Owner Orders</span>
        <h1 class="page-head__title">我的订单</h1>
        <p class="page-head__desc">统一查看寄养订单和挂号订单，在同一页面完成申请、跟进与取消。</p>
      </div>
      <div class="page-head__actions">
        <el-button @click="handleAddAppointment">挂号预约</el-button>
        <el-button type="primary" @click="handleAddBoarding">申请寄养</el-button>
      </div>
    </section>

    <section class="page-panel">
      <div class="section-head">
        <div>
          <h2 class="section-head__title">寄养订单</h2>
          <span class="section-head__hint">当前共 {{ boardingTotal || boardingOrders.length }} 条寄养订单</span>
        </div>
        <span class="metric-pill">房型价格: 标准间 30 / 豪华间 40 / 套房 50</span>
      </div>

      <div class="table-wrap">
        <el-table :data="boardingOrders" v-loading="boardingLoading" class="order-table">
          <el-table-column prop="orderNo" label="订单号" min-width="220" />
          <el-table-column prop="petName" label="宠物" width="120" />
          <el-table-column label="开始日期" width="120">
            <template #default="scope">{{ formatDate(scope.row.startDate) }}</template>
          </el-table-column>
          <el-table-column label="结束日期" width="120">
            <template #default="scope">{{ formatDate(scope.row.endDate) }}</template>
          </el-table-column>
          <el-table-column label="房型" width="110">
            <template #default="scope">{{ roomTypeLabel(scope.row.roomType) }}</template>
          </el-table-column>
          <el-table-column label="总价" width="120">
            <template #default="scope">￥{{ scope.row.priceTotal || 0 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="boardingStatusType(scope.row.status)">{{ boardingStatusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button
                v-if="scope.row.status !== 'CANCELLED' && scope.row.status !== 'COMPLETED'"
                link
                type="danger"
                @click="handleCancelBoarding(scope.row.id)"
              >
                取消订单
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-if="boardingTotal > 0"
        :current-page="boardingPage"
        :page-size="pageSize"
        :total="boardingTotal"
        layout="total, prev, pager, next"
        @current-change="handleBoardingPageChange"
      />
    </section>

    <section class="page-panel">
      <div class="section-head">
        <div>
          <h2 class="section-head__title">挂号订单</h2>
          <span class="section-head__hint">当前共 {{ appointmentTotal || appointments.length }} 条挂号预约</span>
        </div>
        <span class="metric-pill">可在这里直接查看到诊时间、处理结果和预约状态</span>
      </div>

      <div class="table-wrap">
        <el-table :data="appointments" v-loading="appointmentLoading" class="order-table">
          <el-table-column prop="appointmentNo" label="预约号" min-width="220" />
          <el-table-column prop="petName" label="宠物" width="120" />
          <el-table-column label="到诊时间" min-width="180">
            <template #default="scope">{{ formatDateTime(scope.row.appointmentTime) }}</template>
          </el-table-column>
          <el-table-column prop="symptom" label="症状描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="appointmentStatusType(scope.row.status)">{{ appointmentStatusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="result" label="处理结果" min-width="180" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 'PENDING' || scope.row.status === 'CONFIRMED'"
                link
                type="danger"
                @click="handleCancelAppointment(scope.row)"
              >
                取消预约
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-if="appointmentTotal > 0"
        :current-page="appointmentPage"
        :page-size="pageSize"
        :total="appointmentTotal"
        layout="total, prev, pager, next"
        @current-change="handleAppointmentPageChange"
      />
    </section>

    <el-dialog
      v-model="boardingDialogVisible"
      title="申请寄养"
      width="520px"
      align-center
      append-to-body
      destroy-on-close
    >
      <el-form ref="boardingFormRef" :model="boardingForm" :rules="boardingRules" label-width="100px">
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="boardingForm.petId" placeholder="请选择宠物" style="width: 100%">
            <el-option v-for="pet in myPets" :key="pet.id" :label="pet.name" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="寄养日期" prop="dateRange">
          <el-date-picker
            v-model="boardingForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="房型" prop="roomType">
          <el-select v-model="boardingForm.roomType" style="width: 100%">
            <el-option label="标准间 - ￥30/天" value="Standard" />
            <el-option label="豪华间 - ￥40/天" value="Deluxe" />
            <el-option label="套房 - ￥50/天" value="Suite" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估总价">
          <span class="price-value">￥{{ estimatedPrice }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="boardingForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="boardingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="boardingSaving" @click="saveBoardingOrder">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="appointmentDialogVisible"
      title="新增挂号预约"
      width="520px"
      align-center
      append-to-body
      destroy-on-close
    >
      <el-form ref="appointmentFormRef" :model="appointmentForm" :rules="appointmentRules" label-width="100px">
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="appointmentForm.petId" placeholder="请选择宠物" style="width: 100%">
            <el-option v-for="pet in myPets" :key="pet.id" :label="pet.name" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="到诊时间" prop="appointmentTime">
          <el-date-picker
            v-model="appointmentForm.appointmentTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择到诊时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="症状描述" prop="symptom">
          <el-input v-model="appointmentForm.symptom" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="appointmentForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="appointmentSaving" @click="saveAppointmentOrder">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../../utils/request'
import { daysBetweenDates, formatDate, formatDateTime } from '../../utils/date'
import { roomPriceMap, roomTypeLabel } from '../../utils/business'

const route = useRoute()

const myPets = ref<any[]>([])

const pageSize = 10

const boardingOrders = ref<any[]>([])
const boardingLoading = ref(false)
const boardingSaving = ref(false)
const boardingPage = ref(1)
const boardingTotal = ref(0)
const boardingDialogVisible = ref(false)
const boardingFormRef = ref<FormInstance>()

const appointments = ref<any[]>([])
const appointmentLoading = ref(false)
const appointmentSaving = ref(false)
const appointmentPage = ref(1)
const appointmentTotal = ref(0)
const appointmentDialogVisible = ref(false)
const appointmentFormRef = ref<FormInstance>()

const boardingForm = reactive({
  petId: null as number | null,
  dateRange: [] as string[],
  roomType: 'Standard',
  remark: ''
})

const appointmentForm = reactive({
  petId: null as number | null,
  appointmentTime: '',
  symptom: '',
  remark: ''
})

const boardingRules: FormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择寄养日期', trigger: 'change' }],
  roomType: [{ required: true, message: '请选择房型', trigger: 'change' }]
}

const appointmentRules: FormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  appointmentTime: [{ required: true, message: '请选择到诊时间', trigger: 'change' }],
  symptom: [{ required: true, message: '请填写症状描述', trigger: 'blur' }]
}

const boardingStatusLabel = (status: string) =>
  ({
    CREATED: '待确认',
    CONFIRMED: '已确认',
    BOARDING: '寄养中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }[status] || status)

const boardingStatusType = (status: string) =>
  ({
    CREATED: 'info',
    CONFIRMED: 'primary',
    BOARDING: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }[status] || '')

const appointmentStatusLabel = (status: string) =>
  ({
    PENDING: '待确认',
    CONFIRMED: '已确认',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }[status] || status)

const appointmentStatusType = (status: string) =>
  ({
    PENDING: 'warning',
    CONFIRMED: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'info'
  }[status] || '')

const estimatedPrice = computed(() => {
  if (boardingForm.dateRange.length !== 2) return 0
  const [startDate, endDate] = boardingForm.dateRange
  if (!startDate || !endDate) return 0
  const roomPrice = roomPriceMap[boardingForm.roomType] ?? roomPriceMap.Standard ?? 0
  return daysBetweenDates(startDate, endDate) * roomPrice
})

const fetchBoardingOrders = async () => {
  boardingLoading.value = true
  try {
    const res: any = await request.get('/boarding-orders', {
      params: { page: boardingPage.value, size: pageSize }
    })
    boardingOrders.value = res.data?.list || []
    boardingTotal.value = res.data?.total || 0
  } finally {
    boardingLoading.value = false
  }
}

const fetchAppointments = async () => {
  appointmentLoading.value = true
  try {
    const res: any = await request.get('/registration-appointments', {
      params: { page: appointmentPage.value, size: pageSize }
    })
    appointments.value = res.data?.list || []
    appointmentTotal.value = res.data?.total || 0
  } finally {
    appointmentLoading.value = false
  }
}

const fetchPets = async () => {
  const res: any = await request.get('/pets', { params: { size: 999 } })
  myPets.value = res.data?.list || []
}

const resetBoardingForm = () => {
  Object.assign(boardingForm, { petId: null, dateRange: [], roomType: 'Standard', remark: '' })
  boardingFormRef.value?.clearValidate()
}

const resetAppointmentForm = () => {
  Object.assign(appointmentForm, { petId: null, appointmentTime: '', symptom: '', remark: '' })
  appointmentFormRef.value?.clearValidate()
}

const handleBoardingPageChange = (currentPage: number) => {
  boardingPage.value = currentPage
  fetchBoardingOrders()
}

const handleAppointmentPageChange = (currentPage: number) => {
  appointmentPage.value = currentPage
  fetchAppointments()
}

const handleAddBoarding = () => {
  resetBoardingForm()
  boardingDialogVisible.value = true
}

const handleAddAppointment = () => {
  resetAppointmentForm()
  appointmentDialogVisible.value = true
}

const saveBoardingOrder = async () => {
  const valid = await boardingFormRef.value?.validate().catch(() => false)
  if (!valid) return

  boardingSaving.value = true
  try {
    await request.post('/boarding-orders', {
      petId: boardingForm.petId,
      startDate: boardingForm.dateRange[0],
      endDate: boardingForm.dateRange[1],
      roomType: boardingForm.roomType,
      priceTotal: estimatedPrice.value,
      remark: boardingForm.remark
    })
    ElMessage.success('寄养申请已提交')
    boardingDialogVisible.value = false
    await fetchBoardingOrders()
  } finally {
    boardingSaving.value = false
  }
}

const saveAppointmentOrder = async () => {
  const valid = await appointmentFormRef.value?.validate().catch(() => false)
  if (!valid) return

  appointmentSaving.value = true
  try {
    await request.post('/registration-appointments', appointmentForm)
    ElMessage.success('挂号预约已提交')
    appointmentDialogVisible.value = false
    await fetchAppointments()
  } finally {
    appointmentSaving.value = false
  }
}

const handleCancelBoarding = (id: number) => {
  ElMessageBox.confirm('确认取消这条寄养订单吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request.put(`/boarding-orders/${id}/status`, { status: 'CANCELLED' })
      ElMessage.success('订单已取消')
      fetchBoardingOrders()
    })
    .catch(() => {})
}

const handleCancelAppointment = (row: any) => {
  ElMessageBox.confirm('确认取消这条挂号预约吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request.put(`/registration-appointments/${row.id}`, {
        status: 'CANCELLED',
        remark: row.remark
      })
      ElMessage.success('预约已取消')
      fetchAppointments()
    })
    .catch(() => {})
}

const handleRouteAction = (action: unknown) => {
  if (action === 'add') {
    handleAddBoarding()
  }
  if (action === 'registration') {
    handleAddAppointment()
  }
}

onMounted(async () => {
  await Promise.all([fetchBoardingOrders(), fetchAppointments(), fetchPets()])
  handleRouteAction(route.query.action)
})

watch(
  () => route.query.action,
  action => {
    handleRouteAction(action)
  }
)
</script>

<style scoped>
.order-list-page {
  min-width: 0;
}

.table-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  overflow-x: auto;
}

.order-table {
  width: 100%;
  min-width: 900px;
}

.price-value {
  font-size: 18px;
  color: #e6a23c;
  font-weight: bold;
}
</style>
