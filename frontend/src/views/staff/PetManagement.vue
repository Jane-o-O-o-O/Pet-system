<template>
  <div class="page-shell pet-management-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Care Records</span>
        <h1 class="page-head__title">门店宠物管理</h1>
        <p class="page-head__desc">录入病历、疫苗和用药信息，所有日期统一按本地时间显示。</p>
      </div>
    </section>

    <section class="page-panel">
      <div class="page-toolbar">
        <el-input v-model="keyword" placeholder="搜索宠物名" style="width: 220px" @keyup.enter="fetchPets" />
        <el-button type="primary" @click="fetchPets">查询</el-button>
        <span class="panel-topline__label">当前共 {{ total || pets.length }} 条宠物记录</span>
      </div>

      <el-table :data="pets" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名字" />
        <el-table-column prop="species" label="种类" />
        <el-table-column prop="ownerName" label="主人" width="120" />
        <el-table-column label="操作" width="350">
          <template #default="scope">
            <el-button link type="primary" @click="handleRecord(scope.row, 'medical')">录入医疗</el-button>
            <el-button link type="primary" @click="handleRecord(scope.row, 'vaccine')">录入疫苗</el-button>
            <el-button link type="primary" @click="handleViewRecords(scope.row)">查看记录</el-button>
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

    <el-dialog v-model="medicalVisible" :title="editingMedical ? '编辑医疗记录' : '录入医疗记录'" width="550px">
      <el-form ref="medicalFormRef" :model="medicalForm" :rules="medicalRules" label-width="100px">
        <el-form-item label="就诊日期" prop="visitDate">
          <el-date-picker v-model="medicalForm.visitDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="医生"><el-input v-model="medicalForm.doctorName" /></el-form-item>
        <el-form-item label="主诉"><el-input v-model="medicalForm.complaint" type="textarea" /></el-form-item>
        <el-form-item label="诊断"><el-input v-model="medicalForm.diagnosis" type="textarea" /></el-form-item>
        <el-form-item label="治疗/用药"><el-input v-model="medicalForm.treatment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicalVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMedical">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="vaccineVisible" :title="editingVaccine ? '编辑疫苗记录' : '录入疫苗记录'" width="500px">
      <el-form ref="vaccineFormRef" :model="vaccineForm" :rules="vaccineRules" label-width="120px">
        <el-form-item label="疫苗名称" prop="vaccineName"><el-input v-model="vaccineForm.vaccineName" /></el-form-item>
        <el-form-item label="接种日期" prop="shotDate"><el-date-picker v-model="vaccineForm.shotDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="下次接种日期" prop="nextDueDate"><el-date-picker v-model="vaccineForm.nextDueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="提前提醒天数"><el-input-number v-model="vaccineForm.remindDaysBefore" :min="1" :max="30" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vaccineVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVaccine">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordsVisible" :title="`${currentPet?.name || ''} 的记录`" width="80%">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="医疗记录" name="medical">
          <el-table :data="medicalRecords" v-loading="recordsLoading">
            <el-table-column label="就诊日期" width="180">
              <template #default="scope">{{ formatDateTime(scope.row.visitDate) }}</template>
            </el-table-column>
            <el-table-column prop="doctorName" label="医生" width="120" />
            <el-table-column prop="complaint" label="主诉" show-overflow-tooltip />
            <el-table-column prop="diagnosis" label="诊断" show-overflow-tooltip />
            <el-table-column prop="treatment" label="治疗/用药" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button link type="primary" @click="editMedical(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteMedical(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="疫苗记录" name="vaccine">
          <el-table :data="vaccineRecords" v-loading="recordsLoading">
            <el-table-column prop="vaccineName" label="疫苗名称" />
            <el-table-column label="接种日期" width="120">
              <template #default="scope">{{ formatDate(scope.row.shotDate) }}</template>
            </el-table-column>
            <el-table-column label="下次接种" width="120">
              <template #default="scope">{{ formatDate(scope.row.nextDueDate) }}</template>
            </el-table-column>
            <el-table-column label="提醒状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.remindStatus === 'SENT' ? 'success' : 'warning'">
                  {{ { PENDING: '待提醒', SENT: '已提醒', DISABLED: '已禁用' }[scope.row.remindStatus as string] || scope.row.remindStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button link type="primary" @click="editVaccine(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteVaccine(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../../utils/request'
import { formatDate, formatDateTime } from '../../utils/date'

const pets = ref<any[]>([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentPet = ref<any>(null)

const fetchPets = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/pets', { params: { page: page.value, size: pageSize.value, keyword: keyword.value } })
    pets.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
  fetchPets()
}

const medicalVisible = ref(false)
const editingMedical = ref(false)
const medicalFormRef = ref<FormInstance>()
const medicalForm = reactive({ id: null as number | null, visitDate: '', doctorName: '', complaint: '', diagnosis: '', treatment: '' })
const medicalRules: FormRules = { visitDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }] }

const handleRecord = (pet: any, type: string) => {
  currentPet.value = pet
  if (type === 'medical') {
    editingMedical.value = false
    Object.assign(medicalForm, { id: null, visitDate: '', doctorName: '', complaint: '', diagnosis: '', treatment: '' })
    medicalVisible.value = true
  } else {
    editingVaccine.value = false
    Object.assign(vaccineForm, { id: null, vaccineName: '', shotDate: '', nextDueDate: '', remindDaysBefore: 7 })
    vaccineVisible.value = true
  }
}

const saveMedical = async () => {
  const valid = await medicalFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingMedical.value && medicalForm.id) {
      await request.put(`/medical-records/${medicalForm.id}`, medicalForm)
    } else {
      await request.post(`/pets/${currentPet.value.id}/medical-records`, medicalForm)
    }
    ElMessage.success('保存成功')
    medicalVisible.value = false
    if (recordsVisible.value) refreshRecords()
  } finally {
    saving.value = false
  }
}

const editMedical = (row: any) => {
  editingMedical.value = true
  Object.assign(medicalForm, row)
  medicalVisible.value = true
}

const deleteMedical = (id: number) => {
  ElMessageBox.confirm('确认删除这条医疗记录吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/medical-records/${id}`)
    ElMessage.success('删除成功')
    refreshRecords()
  }).catch(() => {})
}

const vaccineVisible = ref(false)
const editingVaccine = ref(false)
const vaccineFormRef = ref<FormInstance>()
const vaccineForm = reactive({ id: null as number | null, vaccineName: '', shotDate: '', nextDueDate: '', remindDaysBefore: 7 })
const vaccineRules: FormRules = {
  vaccineName: [{ required: true, message: '请输入疫苗名称', trigger: 'blur' }],
  shotDate: [{ required: true, message: '请选择接种日期', trigger: 'change' }],
  nextDueDate: [{ required: true, message: '请选择下次接种日期', trigger: 'change' }]
}

const saveVaccine = async () => {
  const valid = await vaccineFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (vaccineForm.nextDueDate && vaccineForm.shotDate && vaccineForm.nextDueDate < vaccineForm.shotDate) {
    ElMessage.warning('下次接种日期不能早于接种日期')
    return
  }
  saving.value = true
  try {
    if (editingVaccine.value && vaccineForm.id) {
      await request.put(`/vaccines/${vaccineForm.id}`, vaccineForm)
    } else {
      await request.post(`/pets/${currentPet.value.id}/vaccines`, vaccineForm)
    }
    ElMessage.success('保存成功')
    vaccineVisible.value = false
    if (recordsVisible.value) refreshRecords()
  } finally {
    saving.value = false
  }
}

const editVaccine = (row: any) => {
  editingVaccine.value = true
  Object.assign(vaccineForm, row)
  vaccineVisible.value = true
}

const deleteVaccine = (id: number) => {
  ElMessageBox.confirm('确认删除这条疫苗记录吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/vaccines/${id}`)
    ElMessage.success('删除成功')
    refreshRecords()
  }).catch(() => {})
}

const recordsVisible = ref(false)
const activeTab = ref('medical')
const medicalRecords = ref<any[]>([])
const vaccineRecords = ref<any[]>([])
const recordsLoading = ref(false)

const handleViewRecords = async (pet: any) => {
  currentPet.value = pet
  recordsVisible.value = true
  await refreshRecords()
}

const refreshRecords = async () => {
  if (!currentPet.value) return
  recordsLoading.value = true
  try {
    const [mrRes, vrRes]: any[] = await Promise.all([
      request.get(`/pets/${currentPet.value.id}/medical-records`),
      request.get(`/pets/${currentPet.value.id}/vaccines`)
    ])
    medicalRecords.value = mrRes.data?.list || []
    vaccineRecords.value = vrRes.data?.list || []
  } finally {
    recordsLoading.value = false
  }
}

onMounted(fetchPets)
</script>
