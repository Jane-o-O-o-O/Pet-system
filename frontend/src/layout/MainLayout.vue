<template>
  <div class="shell">
    <aside class="shell-nav">
      <div class="brand-card">
        <span class="brand-card__eyebrow">Pet Atelier Console</span>
        <h2 class="brand-card__title">毛孩子旅店</h2>
        <p class="brand-card__desc">把寄养、医疗、提醒与客户关系收进一套像精品酒店前台的后台。</p>
        <div class="brand-card__tags">
          <span>寄养排期</span>
          <span>健康档案</span>
          <span>照护提醒</span>
        </div>
      </div>

      <div class="nav-stack">
        <section v-for="group in navGroups" :key="group.title" class="nav-group">
          <p class="nav-group__title">{{ group.title }}</p>
          <button
            v-for="item in group.items"
            :key="item.label"
            type="button"
            class="nav-item"
            :class="{ 'is-active': isActive(item) }"
            @click="navigate(item)"
          >
            <span class="nav-item__icon">
              <component :is="item.icon" />
            </span>
            <span class="nav-item__text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.clue }}</small>
            </span>
          </button>
        </section>
      </div>

      <div class="nav-footnote">
        <span class="nav-footnote__label">当前视角</span>
        <strong>{{ roleLabel }}</strong>
        <p>{{ roleMotto }}</p>
      </div>
    </aside>

    <div class="shell-body">
      <header class="topbar">
        <div class="topbar__lead">
          <el-button class="topbar__drawer-trigger" text @click="mobileNavVisible = true">导航</el-button>
          <div>
            <p class="topbar__eyebrow">{{ currentMoment }}</p>
            <h1 class="topbar__title">{{ currentLabel }}</h1>
            <p class="topbar__desc">{{ currentClue }}</p>
          </div>
        </div>

        <div class="topbar__actions">
          <div class="identity-chip">
            <strong>{{ userStore.username }}</strong>
            <span>{{ roleLabel }}</span>
          </div>
          <el-button text @click="passwordDialogVisible = true">修改密码</el-button>
          <el-button class="logout-button" type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <main class="shell-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <el-drawer v-model="mobileNavVisible" size="320px" title="导航" class="nav-drawer">
      <div class="drawer-nav">
        <section v-for="group in navGroups" :key="group.title" class="nav-group">
          <p class="nav-group__title">{{ group.title }}</p>
          <button
            v-for="item in group.items"
            :key="item.label"
            type="button"
            class="nav-item"
            :class="{ 'is-active': isActive(item) }"
            @click="navigate(item)"
          >
            <span class="nav-item__icon">
              <component :is="item.icon" />
            </span>
            <span class="nav-item__text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.clue }}</small>
            </span>
          </button>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" @close="resetPasswordForm">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password clearable />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="至少 6 个字符" show-password clearable />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitChangePassword">更新密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  CirclePlus,
  DataLine,
  HomeFilled,
  List,
  Management,
  PieChart,
  ShoppingCart,
  Ticket,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import request from '../utils/request'

type NavItem = {
  label: string
  clue: string
  icon: any
  path?: string
  action?: 'addPet' | 'addAppointment'
}

type NavGroup = {
  title: string
  items: NavItem[]
}

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    STAFF: '医护人员',
    OWNER: '宠物主人'
  }
  return map[userStore.role] || userStore.role
})

const roleMotto = computed(() => {
  const map: Record<string, string> = {
    ADMIN: '关注整体经营、排期健康与服务质量。',
    STAFF: '让每一次入住、治疗和交接都井然有序。',
    OWNER: '像记日记一样维护爱宠的全部生活线索。'
  }
  return map[userStore.role] || '管理宠物旅店的每一个细节。'
})

const navGroups = computed<NavGroup[]>(() => {
  const groups: NavGroup[] = [
    {
      title: '总览',
      items: [
        { label: '首页', clue: '全局欢迎页与核心通知', icon: HomeFilled, path: '/dashboard' }
      ]
    }
  ]

  if (userStore.role === 'OWNER') {
    groups.push({
      title: '主人中心',
      items: [
        { label: '新增宠物', clue: '快速建档', icon: CirclePlus, action: 'addPet' },
        { label: '预约寄养', clue: '新建寄养订单', icon: ShoppingCart, action: 'addAppointment' },
        { label: '挂号预约', clue: '提交到诊登记', icon: Ticket, path: '/owner/appointments' },
        { label: '我的宠物', clue: '维护爱宠档案', icon: List, path: '/owner/pets' },
        { label: '我的订单', clue: '跟踪寄养进度', icon: ShoppingCart, path: '/owner/orders' }
      ]
    })
  }

  if (userStore.role === 'STAFF' || userStore.role === 'ADMIN') {
    groups.push({
      title: '门店运营',
      items: [
        { label: '宠物管理', clue: '录入病历与疫苗', icon: Management, path: '/staff/pets' },
        { label: '寄养管理', clue: '处理入住与离店', icon: Ticket, path: '/staff/orders' },
        { label: '挂号预约', clue: '查看并更新预约结果', icon: List, path: '/staff/appointments' },
        { label: '综合统计', clue: '查看库存与房态', icon: DataLine, path: '/staff/operational-stats' }
      ]
    })
  }

  if (userStore.role === 'ADMIN') {
    groups.push({
      title: '系统经营',
      items: [
        { label: '用户管理', clue: '维护角色与状态', icon: User, path: '/admin/users' },
        { label: '统计报表', clue: '查看经营与提醒', icon: PieChart, path: '/admin/stats' }
      ]
    })
  }

  return groups
})

const flatNavItems = computed(() => navGroups.value.flatMap(group => group.items))

const currentItem = computed(() => flatNavItems.value.find(item => item.path === route.path))
const currentLabel = computed(() => currentItem.value?.label || '宠物信息管理系统')
const currentClue = computed(() => currentItem.value?.clue || '让照护、寄养与运营数据在同一界面中流动。')
const currentMoment = computed(() =>
  new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
)

const isActive = (item: NavItem) => item.path === route.path

const mobileNavVisible = ref(false)

const navigate = (item: NavItem) => {
  mobileNavVisible.value = false

  if (item.action === 'addPet') {
    router.push({ path: '/owner/pets', query: { action: 'add' } })
    return
  }

  if (item.action === 'addAppointment') {
    router.push({ path: '/owner/orders', query: { action: 'add' } })
    return
  }

  if (item.path) {
    router.push(item.path)
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
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
    { min: 6, message: '新密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
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
.shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  position: relative;
  z-index: 1;
}

.shell-nav {
  position: sticky;
  top: 0;
  z-index: 4;
  height: 100vh;
  padding: 28px 22px;
  background:
    linear-gradient(180deg, rgba(26, 46, 33, 0.96), rgba(19, 34, 25, 0.98)),
    radial-gradient(circle at top right, rgba(231, 159, 97, 0.24), transparent 28%);
  color: rgba(255, 247, 237, 0.94);
  overflow-y: auto;
}

.shell-nav::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 84% 10%, rgba(255, 255, 255, 0.08), transparent 16%),
    linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.04));
  pointer-events: none;
}

.brand-card,
.nav-footnote {
  position: relative;
  z-index: 1;
  border-radius: 28px;
  padding: 22px;
  background: rgba(255, 248, 235, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.brand-card__eyebrow,
.nav-footnote__label,
.nav-group__title,
.topbar__eyebrow {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.72;
}

.brand-card__title {
  margin: 14px 0 10px;
  font-family: var(--font-display);
  font-size: 34px;
  line-height: 1.05;
}

.brand-card__desc,
.nav-footnote p,
.topbar__desc {
  margin: 0;
  line-height: 1.75;
  color: rgba(255, 247, 237, 0.72);
}

.brand-card__tags {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.brand-card__tags span {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 12px;
}

.nav-stack {
  position: relative;
  z-index: 1;
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-group__title {
  padding-inline: 8px;
}

.nav-item {
  width: 100%;
  border: 0;
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 22px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: transform 180ms ease, background-color 180ms ease, border-color 180ms ease;
}

.nav-item:hover {
  transform: translateX(4px);
  background: rgba(255, 255, 255, 0.06);
}

.nav-item.is-active {
  background: linear-gradient(135deg, rgba(244, 194, 124, 0.24), rgba(255, 255, 255, 0.08));
  box-shadow: inset 0 0 0 1px rgba(255, 222, 184, 0.24);
}

.nav-item__icon {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 18px;
}

.nav-item__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.nav-item__text strong {
  font-size: 15px;
  font-weight: 700;
}

.nav-item__text small {
  color: rgba(255, 247, 237, 0.68);
  line-height: 1.4;
}

.nav-footnote {
  margin-top: 22px;
}

.nav-footnote strong {
  display: block;
  margin: 10px 0 8px;
  font-family: var(--font-display);
  font-size: 28px;
}

.shell-body {
  min-width: 0;
  padding: 22px 24px 30px 0;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  position: relative;
  z-index: 3;
  padding: 18px 26px;
  margin-bottom: 18px;
  border-radius: 32px;
  background: rgba(255, 250, 241, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 18px 50px rgba(49, 35, 20, 0.08);
  backdrop-filter: blur(10px);
}

.topbar__lead {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.topbar__drawer-trigger {
  display: none;
  padding-inline: 12px !important;
}

.topbar__title {
  margin: 4px 0 8px;
  font-family: var(--font-display);
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.08;
}

.topbar__desc {
  max-width: 540px;
  color: var(--pet-muted);
}

.topbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.logout-button {
  color: #fff !important;
  --el-button-text-color: #fff;
  --el-button-hover-text-color: #fff;
  --el-button-active-text-color: #fff;
}

.logout-button:deep(.el-button__text) {
  color: #fff;
}

.identity-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  padding: 12px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(40, 71, 54, 0.08);
}

.identity-chip strong {
  font-size: 14px;
}

.identity-chip span {
  color: var(--pet-muted);
  font-size: 12px;
}

.shell-content {
  position: relative;
  z-index: 1;
}

.drawer-nav {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (max-width: 1180px) {
  .shell {
    grid-template-columns: 280px minmax(0, 1fr);
  }
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .shell-nav {
    display: none;
  }

  .shell-body {
    padding: 18px;
  }

  .topbar {
    padding: 18px 20px;
  }

  .topbar__drawer-trigger {
    display: inline-flex;
  }
}

@media (max-width: 640px) {
  .topbar {
    flex-direction: column;
  }

  .topbar__actions {
    width: 100%;
    justify-content: space-between;
  }

  .identity-chip {
    align-items: flex-start;
  }

  .topbar__title {
    font-size: 30px;
  }
}
</style>
