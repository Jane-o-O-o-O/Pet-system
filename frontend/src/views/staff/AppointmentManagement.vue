<template>
  <div class="page-shell appointment-management-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Clinic Queue</span>
        <h1 class="page-head__title">挂号预约管理</h1>
        <p class="page-head__desc">医护和后台统一查看挂号信息，确认到诊、补充诊疗结果或取消无效预约。</p>
      </div>
    </section>

    <section class="page-panel">
      <div class="page-toolbar">
        <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width: 220px" @change="fetchAppointments">
          <el-option label="待确认" value="PENDING" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <span class="panel-topline__label">当前共 {{ total || appointments.length }} 条预约</span>
      </div>

      <el-table :data="appointments" v-loading="loading" style="width: 100%">
        <el-table-column prop="appointmentNo" label="预约号" min-width="210" />
        <el-table-column prop="petName" label="宠物" width="120" />
        <el-table-column prop="ownerName" label="主人" width="120" />
        <el-table-column label="预约时间" width="170">
          <template #default="scope">{{ formatDateTime(scope.row.appointmentTime) }}</template>
        </el-table-column>
        <el-table-column prop="symptom" label="症状描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="处理结果" min-width="180" show-overflow-tooltip />
        <el-table-column prop="staffName" label="处理人" width="120" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">处理结果</el-button>
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

    <el-dialog v-model="dialogVisible" title="处理挂号预约" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="预约时间">
          <el-date-picker
            v-model="form.appointmentTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预约状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待确认" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="症状描述">
          <el-input v-model="form.symptom" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="form.result" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAppointment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '../../utils/request'
import { formatDateTime } from '../../utils/date'

const appointments = ref<any[]>([])
const statusFilter = ref('')
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: null as number | null,
  appointmentTime: '',
  status: 'PENDING',
  symptom: '',
  result: '',
  remark: ''
})

const rules: FormRules = {
  status: [{ required: true, message: '请选择预约状态', trigger: 'change' }]
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
    const params: any = { page: page.value, size: pageSize.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await request.get('/registration-appointments', { params })
    appointments.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
  fetchAppointments()
}

const openEdit = (row: any) => {
  Object.assign(form, {
    id: row.id,
    appointmentTime: row.appointmentTime,
    status: row.status,
    symptom: row.symptom || '',
    result: row.result || '',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const saveAppointment = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.id) return

  saving.value = true
  try {
    await request.put(`/registration-appointments/${form.id}`, form)
    ElMessage.success('预约结果已更新')
    dialogVisible.value = false
    fetchAppointments()
  } finally {
    saving.value = false
  }
}

onMounted(fetchAppointments)
</script>
