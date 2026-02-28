<template>
  <div class="user-management">
    <div class="header">
      <el-input v-model="keyword" placeholder="搜索用户名/手机号" style="width: 200px; margin-right: 10px;" @keyup.enter="fetchUsers" />
      <el-button type="primary" @click="fetchUsers">查询</el-button>
      <el-button type="success" @click="handleAddUser">创建用户</el-button>
    </div>

    <el-table :data="users" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="角色" width="120">
        <template #default="scope">
          <el-tag :type="roleTagType(scope.row.role)">{{ roleLabel(scope.row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button link type="primary" @click="handleRoleChange(scope.row)">修改角色</el-button>
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

    <!-- Create User Dialog -->
    <el-dialog v-model="createVisible" title="创建用户" width="450px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role">
            <el-option label="服务人员" value="STAFF" />
            <el-option label="宠物主人" value="OWNER" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
      </el-form>
      <div style="color: #999; font-size: 12px; margin-top: 8px;">默认密码为 123456，请通知用户及时修改</div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">创建</el-button>
      </template>
    </el-dialog>

    <!-- Role Change Dialog -->
    <el-dialog v-model="roleVisible" title="修改角色" width="400px">
      <el-select v-model="currentRole" style="width: 100%;">
        <el-option label="管理员" value="ADMIN" />
        <el-option label="服务人员" value="STAFF" />
        <el-option label="宠物主人" value="OWNER" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const users = ref<any[]>([])
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const roleLabel = (r: string) => ({ ADMIN: '管理员', STAFF: '服务人员', OWNER: '宠物主人' }[r] || r)
const roleTagType = (r: string) => ({ ADMIN: 'danger', STAFF: 'warning', OWNER: '' }[r] || '')

const fetchUsers = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/users', { params: { page: page.value, size: pageSize.value, keyword: keyword.value } })
    users.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchUsers()
}

const handleStatusChange = async (row: any) => {
  try {
    await request.put(`/users/${row.id}/status`, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
  }
}

// Create user
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ username: '', role: 'STAFF', phone: '', email: '' })
const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleAddUser = () => {
  Object.assign(createForm, { username: '', role: 'STAFF', phone: '', email: '' })
  createVisible.value = true
}

const saveUser = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await request.post('/users', createForm)
    ElMessage.success('用户创建成功')
    createVisible.value = false
    fetchUsers()
  } catch (error) {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

// Role change
const roleVisible = ref(false)
const currentRole = ref('')
const currentUser = ref<any>(null)

const handleRoleChange = (row: any) => {
  currentUser.value = row
  currentRole.value = row.role
  roleVisible.value = true
}

const saveRole = async () => {
  saving.value = true
  try {
    await request.put(`/users/${currentUser.value.id}/role`, { role: currentRole.value })
    ElMessage.success('角色更新成功')
    roleVisible.value = false
    fetchUsers()
  } catch (error) {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(fetchUsers)
</script>

<style scoped>
.header { margin-bottom: 20px; display: flex; align-items: center; }
</style>
