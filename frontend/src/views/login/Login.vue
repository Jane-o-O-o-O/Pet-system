<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>{{ isLogin ? '用户登录' : '用户注册' }}</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <template v-if="!isLogin">
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">{{ isLogin ? '登录' : '注册' }}</el-button>
          <el-button link @click="toggleMode">{{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import request from '../../utils/request'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isLogin = ref(true)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: ''
})

const rules = computed<FormRules>(() => {
  const base: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
  if (!isLogin.value) {
    base.password.push({ min: 6, message: '密码至少6个字符', trigger: 'blur' })
    base.confirmPassword = [
      { required: true, message: '请再次输入密码', trigger: 'blur' },
      {
        validator: (_rule: any, value: string, callback: any) => {
          if (value !== form.password) callback(new Error('两次输入的密码不一致'))
          else callback()
        },
        trigger: 'blur'
      }
    ]
    base.email = [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
  }
  return base
})

const toggleMode = () => {
  isLogin.value = !isLogin.value
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (isLogin.value) {
      const res: any = await request.post('/auth/login', {
        username: form.username,
        password: form.password
      })
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.role, res.data.userId, res.data.username)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      await request.post('/auth/register', {
        username: form.username,
        password: form.password,
        phone: form.phone,
        email: form.email
      })
      ElMessage.success('注册成功，请登录')
      isLogin.value = true
    }
  } catch (error) {
    // error already handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(160deg, #f5f3ef 0%, #e8e6e1 50%, #ddd9d2 100%);
}
.login-card {
  width: 420px;
  border-radius: 8px;
}
.card-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
}
</style>
