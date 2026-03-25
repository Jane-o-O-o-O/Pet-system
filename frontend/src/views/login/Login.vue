<template>
  <div class="login-shell">
    <section class="login-story">
      <span class="login-story__eyebrow">Boutique Pet Care</span>
      <h1 class="login-story__title">把每只毛孩子的寄养、健康与陪伴，做成一套会呼吸的档案。</h1>
      <p class="login-story__desc">
        它既是宠物酒店的前台，也是照护记录册。主人、门店与管理员都在同一条时间线上协作。
      </p>

      <div class="login-story__tags">
        <span>猫咪寄宿</span>
        <span>犬类护理</span>
        <span>疫苗提醒</span>
        <span>房态排期</span>
      </div>

      <div class="story-grid">
        <article class="story-card story-card--tall">
          <span>今日氛围</span>
          <strong>07</strong>
          <p>只宠物在住，全部照护节点都可追踪。</p>
        </article>
        <article class="story-card">
          <span>照护方式</span>
          <strong>一宠一档</strong>
          <p>档案、病历、疫苗提醒与入住记录自动串联。</p>
        </article>
        <article class="story-card">
          <span>服务体验</span>
          <strong>精品旅店</strong>
          <p>用更有温度的界面处理本来冰冷的后台事务。</p>
        </article>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-panel__head">
        <span class="login-panel__eyebrow">Pet Atelier Console</span>
        <h2>{{ isLogin ? '欢迎回到毛孩子旅店' : '创建新的照护档案' }}</h2>
        <p>{{ isLogin ? '登录后继续查看宠物动态、寄养订单与提醒。' : '注册后即可开始建立宠物信息与预约记录。' }}</p>
      </div>

      <div class="mode-switch" role="tablist" aria-label="登录方式切换">
        <button type="button" :class="{ 'is-active': isLogin }" @click="switchMode(true)">登录</button>
        <button type="button" :class="{ 'is-active': !isLogin }" @click="switchMode(false)">注册</button>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
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
          <div class="login-form__grid">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </div>
        </template>

        <div class="login-actions">
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            {{ isLogin ? '进入系统' : '提交注册' }}
          </el-button>
          <button type="button" class="text-switch" @click="switchMode(!isLogin)">
            {{ isLogin ? '还没有账号？立即注册' : '已经有账号？返回登录' }}
          </button>
        </div>
      </el-form>

      <div class="login-notes">
        <span>档案同步</span>
        <span>寄养追踪</span>
        <span>健康提醒</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '../../store/user'
import request from '../../utils/request'

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
  const passwordRules = Array.isArray(base.password) ? [...base.password] : base.password ? [base.password] : []

  if (!isLogin.value) {
    passwordRules.push({ min: 6, message: '密码至少 6 个字符', trigger: 'blur' })
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

  base.password = passwordRules
  return base
})

const switchMode = (loginMode: boolean) => {
  isLogin.value = loginMode
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
      switchMode(true)
    }
  } catch (error) {
    // error already handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.15fr minmax(420px, 520px);
  gap: 28px;
  padding: 30px;
  position: relative;
  z-index: 1;
}

.login-shell::before {
  content: "";
  position: absolute;
  inset: 24px;
  border-radius: 40px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.52), transparent 45%),
    radial-gradient(circle at 14% 18%, rgba(231, 159, 97, 0.22), transparent 20%);
  pointer-events: none;
}

.login-story,
.login-panel {
  position: relative;
  z-index: 1;
  border-radius: 36px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(49, 35, 20, 0.12);
}

.login-story {
  padding: 48px;
  background:
    linear-gradient(160deg, rgba(30, 55, 41, 0.96), rgba(19, 34, 25, 0.98)),
    radial-gradient(circle at top right, rgba(231, 159, 97, 0.3), transparent 28%);
  color: rgba(255, 248, 235, 0.94);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.login-story__eyebrow,
.login-panel__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.login-story__eyebrow {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.login-story__title {
  max-width: 720px;
  margin: 24px 0 18px;
  font-family: var(--font-display);
  font-size: clamp(44px, 5vw, 74px);
  line-height: 1.04;
}

.login-story__desc {
  max-width: 580px;
  margin: 0;
  line-height: 1.9;
  font-size: 16px;
  color: rgba(255, 248, 235, 0.76);
}

.login-story__tags {
  margin-top: 26px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.login-story__tags span {
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(6px);
}

.story-grid {
  margin-top: 40px;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 16px;
}

.story-card {
  min-height: 170px;
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 248, 235, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.story-card--tall {
  grid-row: span 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(231, 159, 97, 0.16)),
    rgba(255, 248, 235, 0.08);
}

.story-card span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.72;
}

.story-card strong {
  display: block;
  margin: 18px 0 10px;
  font-family: var(--font-display);
  font-size: clamp(30px, 4vw, 54px);
  line-height: 1;
}

.story-card p {
  margin: 0;
  line-height: 1.7;
  color: rgba(255, 248, 235, 0.72);
}

.login-panel {
  padding: 34px 32px;
  background: rgba(255, 250, 241, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(12px);
}

.login-panel__head h2 {
  margin: 18px 0 10px;
  font-family: var(--font-display);
  font-size: 38px;
  line-height: 1.1;
}

.login-panel__head p {
  margin: 0;
  line-height: 1.8;
  color: var(--pet-muted);
}

.login-panel__eyebrow {
  background: rgba(40, 71, 54, 0.08);
  border: 1px solid rgba(40, 71, 54, 0.1);
  color: var(--pet-forest);
}

.mode-switch {
  margin: 26px 0 22px;
  padding: 6px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  border-radius: 999px;
  background: rgba(40, 71, 54, 0.06);
}

.mode-switch button,
.text-switch {
  border: 0;
  background: transparent;
  cursor: pointer;
  color: inherit;
}

.mode-switch button {
  padding: 12px 16px;
  border-radius: 999px;
  font-weight: 700;
  transition: background-color 180ms ease, color 180ms ease, transform 180ms ease;
}

.mode-switch button.is-active {
  background: linear-gradient(135deg, var(--pet-forest), var(--pet-forest-deep));
  color: #fff9f0;
  box-shadow: 0 10px 22px rgba(40, 71, 54, 0.2);
}

.login-form {
  margin-top: 10px;
}

.login-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.login-actions {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.login-actions :deep(.el-button) {
  width: 100%;
  height: 50px;
}

.text-switch {
  padding: 10px 6px 0;
  text-align: center;
  color: var(--pet-forest);
  font-weight: 600;
}

.login-notes {
  margin-top: 28px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.login-notes span {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(40, 71, 54, 0.08);
  color: var(--pet-muted);
  font-size: 13px;
}

@media (max-width: 1180px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-story {
    min-height: 520px;
  }
}

@media (max-width: 720px) {
  .login-shell {
    padding: 16px;
    gap: 16px;
  }

  .login-shell::before {
    inset: 12px;
    border-radius: 28px;
  }

  .login-story,
  .login-panel {
    border-radius: 28px;
  }

  .login-story,
  .login-panel {
    padding: 24px;
  }

  .story-grid,
  .login-form__grid {
    grid-template-columns: 1fr;
  }

  .login-story__title {
    font-size: 42px;
  }

  .login-panel__head h2 {
    font-size: 32px;
  }
}
</style>
