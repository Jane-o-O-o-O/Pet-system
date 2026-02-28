<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">宠物管理系统</div>
      <el-menu
        :default-active="$route.path"
        router
        class="el-menu-vertical"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <template v-if="userStore.role === 'OWNER'">
          <el-menu-item index="" @click="goToPetList">
            <el-icon><CirclePlus /></el-icon>
            <span>新增宠物</span>
          </el-menu-item>
          <el-menu-item index="" @click="goToOrderList">
            <el-icon><ShoppingCart /></el-icon>
            <span>申请寄养</span>
          </el-menu-item>
          <el-menu-item index="/owner/pets">
            <el-icon><List /></el-icon>
            <span>我的宠物</span>
          </el-menu-item>
          <el-menu-item index="/owner/orders">
            <el-icon><ShoppingCart /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
        </template>

        <template v-if="userStore.role === 'STAFF' || userStore.role === 'ADMIN'">
          <el-menu-item index="/staff/pets">
            <el-icon><Management /></el-icon>
            <span>宠物管理</span>
          </el-menu-item>
          <el-menu-item index="/staff/orders">
            <el-icon><Ticket /></el-icon>
            <span>寄养管理</span>
          </el-menu-item>
          <el-menu-item index="/staff/operational-stats">
            <el-icon><DataLine /></el-icon>
            <span>综合统计</span>
          </el-menu-item>
        </template>

        <template v-if="userStore.role === 'ADMIN'">
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/stats">
            <el-icon><PieChart /></el-icon>
            <span>统计报表</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left"></div>
        <div class="header-right">
          <span>{{ userStore.username }} ({{ roleLabel }})</span>
          <el-button link @click="passwordDialogVisible = true">修改密码</el-button>
          <el-button link @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px" @close="resetPasswordForm">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password clearable />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="至少6个字符" show-password clearable />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useUserStore } from '../store/user'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { HomeFilled, List, ShoppingCart, Management, Ticket, User, PieChart, DataLine, CirclePlus, Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

const roleLabel = computed(() => {
  const map: Record<string, string> = { ADMIN: '管理员', STAFF: '服务人员', OWNER: '宠物主人' }
  return map[userStore.role] || userStore.role
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const goToPetList = () => {
  router.push({ path: '/owner/pets', query: { action: 'add' } })
}

const goToOrderList = () => {
  router.push({ path: '/owner/orders', query: { action: 'add' } })
}

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const submitChangePassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await request.put('/users/me/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码登录')
    passwordDialogVisible.value = false
    resetPasswordForm()
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
  border-bottom: 1px solid #e6e6e6;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ddd;
  background-color: #fff;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.el-menu-vertical {
  height: calc(100% - 60px);
  border-right: none;
}
</style>
