<template>
  <div class="page-shell appointment-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Registration Desk</span>
        <h1 class="page-head__title">挂号预约</h1>
        <p class="page-head__desc">在线提交到诊时间、宠物与症状信息，医护人员确认后可继续补充处理结果。</p>
      </div>
      <div class="page-head__actions">
        <el-button type="primary" @click="handleAdd">新增预约</el-button>
      </div>
    </section>

    <section class="page-panel">
      <div class="panel-topline">
        <span class="panel-topline__label">当前共 {{ total || appointments.length }} 条挂号预约</span>
        <span class="panel-topline__label">支持主人取消待处理预约，医护和后台可跟进结果</span>
      </div>

      <el-table :data="appointments" v-loading="loading" style="width: 100%">
        <el-table-column prop="appointmentNo" label="预约号" width="220" />
        <el-table-column prop="petName" label="宠物" width="120" />
        <el-table-column label="预约时间" width="170">
          <template #default="scope">{{ formatDateTime(scope.row.appointmentTime) }}</template>
        </el-table-column>
        <el-table-column prop="symptom" label="症状描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="处理结果" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING' || scope.row.status === 'CONFIRMED'"
              link
              type="danger"
              @click="cancelAppointment(scope.row)"
            >
              取消预约
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

    <el-dialog
      v-model="dialogVisible"
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAppointment">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../../utils/request'
import { formatDateTime } from '../../utils/date'

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
  appointmentTime: '',
  symptom: '',
  remark: ''
})

const appointmentRules: FormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  appointmentTime: [{ required: true, message: '请选择到诊时间', trigger: 'change' }],
  symptom: [{ required: true, message: '请填写症状描述', trigger: 'blur' }]
}

const statusLabel = (status: string) => ({
  PENDING: '待确认',
  CONFIRMED: '已确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}[status] || status)

const statusType = (status: string) => ({
  PENDING: 'warning',
  CONFIRMED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info'
}[status] || '')

const fetchAppointments = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/registration-appointments', { params: { page: page.value, size: pageSize.value } })
    appointments.value = res.data?.list || []
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
  fetchAppointments()
}

const handleAdd = () => {
  Object.assign(appointmentForm, {
    petId: null,
    appointmentTime: '',
    symptom: '',
    remark: ''
  })
  dialogVisible.value = true
}

const saveAppointment = async () => {
  const valid = await appointmentFormRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await request.post('/registration-appointments', appointmentForm)
    ElMessage.success('预约提交成功')
    dialogVisible.value = false
    fetchAppointments()
  } finally {
    saving.value = false
  }
}

const cancelAppointment = (row: any) => {
  ElMessageBox.confirm('确认取消这条预约吗？', '提示', { type: 'warning' }).then(async () => {
    await request.put(`/registration-appointments/${row.id}`, {
      status: 'CANCELLED',
      remark: row.remark
    })
    ElMessage.success('预约已取消')
    fetchAppointments()
  }).catch(() => {})
}

onMounted(async () => {
  await Promise.all([fetchAppointments(), fetchPets()])
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
