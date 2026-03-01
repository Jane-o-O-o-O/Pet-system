<template>
  <div class="appointment-list">
    <div class="header">
      <el-button type="primary" @click="handleAdd">新增预约</el-button>
    </div>

    <el-table :data="appointments" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderNo" label="预约号" width="230" />
      <el-table-column prop="petName" label="宠物" width="120" />
      <el-table-column prop="startDate" label="入住日期" width="120" />
      <el-table-column prop="endDate" label="离店日期" width="120" />
      <el-table-column prop="roomType" label="房型" width="100">
        <template #default="scope">{{ roomTypeLabel(scope.row.roomType) }}</template>
      </el-table-column>
      <el-table-column label="预估价格" width="110">
        <template #default="scope">¥{{ scope.row.priceTotal || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'CREATED' || scope.row.status === 'CONFIRMED'"
            link
            type="danger"
            @click="handleCancel(scope.row.id)"
          >取消预约</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增预约" width="500px">
      <el-form ref="appointmentFormRef" :model="appointmentForm" :rules="appointmentRules" label-width="100px">
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="appointmentForm.petId" placeholder="请选择宠物">
            <el-option v-for="pet in myPets" :key="pet.id" :label="pet.name" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" prop="dateRange">
          <el-date-picker
            v-model="appointmentForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="入住日期"
            end-placeholder="离店日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="房型" prop="roomType">
          <el-select v-model="appointmentForm.roomType">
            <el-option label="标准间 - ¥100/天" value="Standard" />
            <el-option label="豪华间 - ¥200/天" value="Deluxe" />
            <el-option label="套房 - ¥300/天" value="Suite" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估总价">
          <span class="price-value">¥{{ estimatedPrice }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="appointmentForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAppointment">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../../utils/request'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

const route = useRoute()

const appointments = ref<any[]>([])
const myPets = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const appointmentFormRef = ref<FormInstance>()

const appointmentForm = reactive({
  petId: null as number | null,
  dateRange: [] as string[],
  roomType: 'Standard',
  remark: ''
})

const appointmentRules: FormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  roomType: [{ required: true, message: '请选择房型', trigger: 'change' }]
}

const roomPriceMap: Record<string, number> = { Standard: 100, Deluxe: 200, Suite: 300 }
const roomTypeLabel = (type: string) => ({ Standard: '标准间', Deluxe: '豪华间', Suite: '套房' }[type] || type)
const statusLabel = (status: string) => ({ CREATED: '待确认', CONFIRMED: '已预约', BOARDING: '已入住', COMPLETED: '已完成', CANCELLED: '已取消' }[status] || status)
const getStatusType = (status: string) => ({ CREATED: 'info', CONFIRMED: 'success', BOARDING: 'warning', COMPLETED: '', CANCELLED: 'danger' }[status] || '')

const estimatedPrice = computed(() => {
  if (appointmentForm.dateRange?.length === 2) {
    const start = appointmentForm.dateRange[0]
    const end = appointmentForm.dateRange[1]
    if (typeof start !== 'string' || typeof end !== 'string') return 0
    const startDate = new Date(start).getTime()
    const endDate = new Date(end).getTime()
    const days = Math.max(1, Math.ceil((endDate - startDate) / 86400000))
    return days * (roomPriceMap[appointmentForm.roomType] || 100)
  }
  return 0
})

const fetchAppointments = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/boarding-orders', { params: { page: page.value, size: pageSize.value } })
    appointments.value = res.data?.list || []
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

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
  fetchAppointments()
}

const handleAdd = () => {
  fetchPets()
  Object.assign(appointmentForm, { petId: null, dateRange: [], roomType: 'Standard', remark: '' })
  dialogVisible.value = true
}

const saveAppointment = async () => {
  const valid = await appointmentFormRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await request.post('/boarding-orders', {
      petId: appointmentForm.petId,
      startDate: appointmentForm.dateRange[0],
      endDate: appointmentForm.dateRange[1],
      roomType: appointmentForm.roomType,
      priceTotal: estimatedPrice.value,
      remark: appointmentForm.remark
    })
    ElMessage.success('预约成功')
    dialogVisible.value = false
    fetchAppointments()
  } catch (error) {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

const handleCancel = (id: number) => {
  ElMessageBox.confirm('确定取消该预约吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.put(`/boarding-orders/${id}/status`, { status: 'CANCELLED' })
      ElMessage.success('预约已取消')
      fetchAppointments()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchAppointments()
  fetchPets()
  if (route.query.action === 'add') {
    handleAdd()
  }
})

watch(() => route.query.action, (action) => {
  if (action === 'add') {
    handleAdd()
  }
})
</script>

<style scoped>
.header {
  margin-bottom: 20px;
}

.price-value {
  font-size: 18px;
  color: #e6a23c;
  font-weight: bold;
}
</style>
