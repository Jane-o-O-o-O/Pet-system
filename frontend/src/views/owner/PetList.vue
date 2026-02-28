<template>
  <div class="pet-list">
    <div class="header">
      <el-button type="primary" @click="handleAdd">新增宠物</el-button>
    </div>

    <el-table :data="pets" v-loading="loading" style="width: 100%">
      <el-table-column prop="name" label="名字" />
      <el-table-column prop="species" label="种类" />
      <el-table-column prop="breed" label="品种" />
      <el-table-column label="性别" width="80">
        <template #default="scope">{{ genderLabel(scope.row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="birthDate" label="生日" width="120" />
      <el-table-column prop="weight" label="体重(kg)" width="100" />
      <el-table-column label="是否绝育" width="100">
        <template #default="scope">{{ scope.row.sterilized ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="scope">
          <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button link type="primary" @click="handleViewRecords(scope.row)">医疗/疫苗</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
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

    <!-- Pet Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑宠物' : '新增宠物'" width="500px">
      <el-form ref="petFormRef" :model="petForm" :rules="petRules" label-width="80px">
        <el-form-item label="名字" prop="name">
          <el-input v-model="petForm.name" />
        </el-form-item>
        <el-form-item label="种类" prop="species">
          <el-input v-model="petForm.species" />
        </el-form-item>
        <el-form-item label="品种">
          <el-input v-model="petForm.breed" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="petForm.gender">
            <el-option label="公" value="M" />
            <el-option label="母" value="F" />
            <el-option label="未知" value="U" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="petForm.birthDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="petForm.weight" :precision="2" :step="0.1" :min="0" />
        </el-form-item>
        <el-form-item label="是否绝育">
          <el-switch v-model="petForm.sterilized" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="petForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePet">确定</el-button>
      </template>
    </el-dialog>

    <!-- Records Dialog -->
    <el-dialog v-model="recordsVisible" :title="currentPet?.name + ' 的记录'" width="70%">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="医疗记录" name="medical">
          <el-table :data="medicalRecords" v-loading="recordsLoading">
            <el-table-column prop="visitDate" label="就诊日期" width="180" />
            <el-table-column prop="doctorName" label="医生" width="120" />
            <el-table-column prop="complaint" label="主诉" show-overflow-tooltip />
            <el-table-column prop="diagnosis" label="诊断" show-overflow-tooltip />
            <el-table-column prop="treatment" label="治疗" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="疫苗记录" name="vaccine">
          <el-table :data="vaccineRecords" v-loading="recordsLoading">
            <el-table-column prop="vaccineName" label="疫苗名称" />
            <el-table-column prop="shotDate" label="接种日期" width="120" />
            <el-table-column prop="nextDueDate" label="下次接种" width="120" />
            <el-table-column label="提醒状态" width="100">
              <template #default="scope">
                <el-tag :type="remindStatusType(scope.row.remindStatus)">{{ remindStatusLabel(scope.row.remindStatus) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

const pets = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const petFormRef = ref<FormInstance>()

const petForm = reactive({
  id: null as number | null,
  name: '',
  species: '',
  breed: '',
  gender: 'U',
  birthDate: '',
  weight: 0,
  sterilized: 0,
  remark: ''
})

const petRules: FormRules = {
  name: [{ required: true, message: '请输入宠物名字', trigger: 'blur' }],
  species: [{ required: true, message: '请输入宠物种类', trigger: 'blur' }]
}

const genderLabel = (g: string) => ({ M: '公', F: '母', U: '未知' }[g] || g)
const remindStatusLabel = (s: string) => ({ PENDING: '待提醒', SENT: '已提醒', DISABLED: '已禁用' }[s] || s)
const remindStatusType = (s: string) => ({ PENDING: 'warning', SENT: 'success', DISABLED: 'info' }[s] || '')

const fetchPets = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/pets', { params: { page: page.value, size: pageSize.value } })
    pets.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchPets()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(petForm, { id: null, name: '', species: '', breed: '', gender: 'U', birthDate: '', weight: 0, sterilized: 0, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  Object.assign(petForm, row)
  dialogVisible.value = true
}

const savePet = async () => {
  const valid = await petFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/pets/${petForm.id}`, petForm)
    } else {
      await request.post('/pets', petForm)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchPets()
  } catch (error) {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该宠物吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/pets/${id}`)
      ElMessage.success('删除成功')
      fetchPets()
    } catch (error) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const recordsVisible = ref(false)
const activeTab = ref('medical')
const currentPet = ref<any>(null)
const medicalRecords = ref<any[]>([])
const vaccineRecords = ref<any[]>([])
const recordsLoading = ref(false)

const handleViewRecords = async (pet: any) => {
  currentPet.value = pet
  recordsVisible.value = true
  recordsLoading.value = true
  try {
    const [mrRes, vrRes]: any[] = await Promise.all([
      request.get(`/pets/${pet.id}/medical-records`),
      request.get(`/pets/${pet.id}/vaccines`)
    ])
    medicalRecords.value = mrRes.data?.list || []
    vaccineRecords.value = vrRes.data?.list || []
  } catch (error) {
    // handled by interceptor
  } finally {
    recordsLoading.value = false
  }
}

onMounted(fetchPets)
</script>

<style scoped>
.header { margin-bottom: 20px; }
</style>
