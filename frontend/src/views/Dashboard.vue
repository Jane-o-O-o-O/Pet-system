<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>{{ welcomeTitle }}, {{ userStore.username }}!</span>
        </div>
      </template>
      <div class="welcome-meta">
        <span class="meta-item">角色：{{ roleLabel }}</span>
        <span class="meta-item">当前时间：{{ currentTime }}</span>
      </div>
    </el-card>

    <!-- 角色专属概览区 -->
    <template v-if="userStore.role === 'OWNER'">
      <div class="section-title">我的概览</div>
      <el-row :gutter="16" class="overview-row">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ ownerStats.petCount }}</div>
            <div class="stat-label">我的宠物</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ ownerStats.vaccineRemindCount }}</div>
            <div class="stat-label">待接种提醒</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ ownerStats.activeOrderCount }}</div>
            <div class="stat-label">进行中订单</div>
          </el-card>
        </el-col>
      </el-row>
      <div class="section-title">快捷操作</div>
      <el-card class="quick-actions-card">
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="action-button" @click="navigateToPets">
              <el-icon class="action-icon pet"><Plus /></el-icon>
              <div class="action-content">
                <div class="action-title">新增宠物</div>
                <div class="action-desc">添加新的宠物档案</div>
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="action-button" @click="navigateToOrders">
              <el-icon class="action-icon order"><Calendar /></el-icon>
              <div class="action-content">
                <div class="action-title">申请寄养</div>
                <div class="action-desc">为宠物预订寄养服务</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
      <div class="section-title">待办与提醒</div>
      <el-card class="message-list-card">
        <ul class="virtual-messages">
          <li v-for="(msg, i) in ownerMessages" :key="i" class="message-item">
            <el-icon class="msg-icon"><Bell /></el-icon>
            <span>{{ msg }}</span>
          </li>
        </ul>
      </el-card>
    </template>

    <template v-if="userStore.role === 'STAFF'">
      <div class="section-title">今日工作台</div>
      <el-row :gutter="16" class="overview-row">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ staffStats.todayCheckIn }}</div>
            <div class="stat-label">今日入住</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ staffStats.pendingConfirm }}</div>
            <div class="stat-label">待确认订单</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ staffStats.boardingNow }}</div>
            <div class="stat-label">在店寄养</div>
          </el-card>
        </el-col>
      </el-row>
      <div class="section-title">待办事项</div>
      <el-card class="message-list-card">
        <ul class="virtual-messages">
          <li v-for="(msg, i) in staffMessages" :key="i" class="message-item">
            <el-icon class="msg-icon"><List /></el-icon>
            <span>{{ msg }}</span>
          </li>
        </ul>
      </el-card>
    </template>

    <template v-if="userStore.role === 'ADMIN'">
      <div class="section-title">运营概览</div>
      <el-row :gutter="16" class="overview-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card admin">
            <div class="stat-value">{{ adminStats.userCount }}</div>
            <div class="stat-label">注册用户</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card admin">
            <div class="stat-value">{{ adminStats.petCount }}</div>
            <div class="stat-label">宠物档案</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card admin">
            <div class="stat-value">{{ adminStats.orderCount }}</div>
            <div class="stat-label">寄养订单</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card admin">
            <div class="stat-value">{{ adminStats.todayOrders }}</div>
            <div class="stat-label">今日订单</div>
          </el-card>
        </el-col>
      </el-row>
      <div class="section-title">近期动态</div>
      <el-card class="message-list-card">
        <ul class="virtual-messages">
          <li v-for="(msg, i) in adminMessages" :key="i" class="message-item">
            <el-icon class="msg-icon"><DataLine /></el-icon>
            <span>{{ msg }}</span>
          </li>
        </ul>
      </el-card>
    </template>

    <!-- 系统通知 -->
    <div class="section-title">系统通知</div>
    <el-card class="notify-card">
      <el-table :data="displayNotifications" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="120" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button v-if="scope.row.readFlag === 0 && scope.row.id" link type="primary" @click="markAsRead(scope.row.id)">标记已读</el-button>
            <span v-else-if="scope.row.id" style="color: #999">已读</span>
            <span v-else style="color: #999">—</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > pageSize"
        style="margin-top: 16px; justify-content: flex-end;"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { Bell, List, DataLine, Plus, Calendar } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const currentTime = ref(new Date().toLocaleString())
const notifications = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const roleLabel = computed(() => {
  const map: Record<string, string> = { ADMIN: '管理员', STAFF: '服务人员', OWNER: '宠物主人' }
  return map[userStore.role] || userStore.role
})

const welcomeTitle = computed(() => {
  const map: Record<string, string> = { ADMIN: '欢迎回来', STAFF: '欢迎回来', OWNER: '欢迎回来' }
  return map[userStore.role] || '欢迎回来'
})

// 宠物主人：概览为真实数据，由 fetchOverview 拉取
const ownerStats = ref({ petCount: 0, vaccineRemindCount: 0, activeOrderCount: 0 })
const ownerMessages = ref([
  '您有一条疫苗即将到期提醒，请及时在「我的宠物」中查看并预约。',
  '完善宠物档案可获得更精准的寄养与健康建议。',
  '新用户首次寄养可享会员优惠，详情见订单页。'
])

// 服务人员：概览为真实数据
const staffStats = ref({ todayCheckIn: 0, pendingConfirm: 0, boardingNow: 0 })
const staffMessages = ref([
  '今日有 2 笔订单待确认，请尽快联系主人完成确认。',
  '在店寄养宠物中有 1 只即将到期，请提前与主人沟通续期或接回。',
  '请每日检查在店宠物状态并更新照护记录。'
])

// 管理员：概览为真实数据（接口返回 users, pets, orders, todayOrders）
const adminStats = ref({ userCount: 0, petCount: 0, orderCount: 0, todayOrders: 0 })
const adminMessages = ref([
  '昨日新增注册用户 3 人，宠物档案 2 条。',
  '本月寄养订单较上月增长 12%，请关注库存与排期。',
  '系统运行正常，未发现异常登录或安全告警。'
])

// 无真实通知时按角色展示的虚拟通知（仅用于展示，无 id 的不显示标记已读）
const virtualNotificationsByRole: Record<string, any[]> = {
  OWNER: [
    { title: '疫苗提醒', content: '您的爱宠「小白」的狂犬疫苗将于 7 日后到期，请及时接种。', createdAt: new Date().toLocaleString(), readFlag: 0, id: null },
    { title: '服务说明', content: '寄养订单确认后请按约定时间送达，如有变更请提前联系门店。', createdAt: new Date().toLocaleString(), readFlag: 1, id: null }
  ],
  STAFF: [
    { title: '待办提醒', content: '订单 #BO20260225001 待确认，请今日内联系主人完成确认。', createdAt: new Date().toLocaleString(), readFlag: 0, id: null },
    { title: '交接说明', content: '新入住宠物「豆豆」已完成入住登记，请按照护须知执行。', createdAt: new Date().toLocaleString(), readFlag: 1, id: null }
  ],
  ADMIN: [
    { title: '数据日报', content: '昨日新增用户 3 人、订单 2 笔，系统运行正常。', createdAt: new Date().toLocaleString(), readFlag: 1, id: null },
    { title: '系统公告', content: '本周六 00:00–02:00 进行例行维护，请提前通知相关用户。', createdAt: new Date().toLocaleString(), readFlag: 0, id: null }
  ]
}

const displayNotifications = computed(() => {
  const list = notifications.value && notifications.value.length > 0
    ? notifications.value
    : (virtualNotificationsByRole[userStore.role] || [])
  return list.map((row: any) => ({
    ...row,
    createdAt: row.createdAt || (row.created_at ? new Date(row.created_at).toLocaleString() : '—')
  }))
})

const fetchNotifications = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/notifications', { params: { page: page.value, size: pageSize.value } })
    notifications.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    notifications.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handlePageChange = (p: number) => {
  page.value = p
  fetchNotifications()
}

const markAsRead = async (id: number) => {
  try {
    await request.put(`/notifications/${id}/read`)
    fetchNotifications()
  } catch (error) {
    // handled by interceptor
  }
}

const navigateToPets = () => {
  router.push('/owner/pets')
}

const navigateToOrders = () => {
  router.push('/owner/orders')
}

const fetchOverview = async () => {
  try {
    const res: any = await request.get('/dashboard/overview')
    const d = res.data || {}
    if (userStore.role === 'OWNER') {
      ownerStats.value = {
        petCount: d.petCount ?? 0,
        vaccineRemindCount: d.vaccineRemindCount ?? 0,
        activeOrderCount: d.activeOrderCount ?? 0
      }
    } else if (userStore.role === 'STAFF') {
      staffStats.value = {
        todayCheckIn: d.todayCheckIn ?? 0,
        pendingConfirm: d.pendingConfirm ?? 0,
        boardingNow: d.boardingNow ?? 0
      }
    } else if (userStore.role === 'ADMIN') {
      adminStats.value = {
        userCount: d.users ?? 0,
        petCount: d.pets ?? 0,
        orderCount: d.orders ?? 0,
        todayOrders: d.todayOrders ?? 0
      }
    }
  } catch (error) {
    // use default 0 values
  }
}

onMounted(() => {
  fetchOverview()
  fetchNotifications()
  timer = setInterval(() => {
    currentTime.value = new Date().toLocaleString()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.welcome-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
}
.welcome-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}
.meta-item {
  color: #666;
  font-size: 14px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 2px;
}
.overview-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  padding: 16px 0;
}
.stat-card .stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}
.stat-card .stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
.stat-card.admin .stat-value {
  color: #67c23a;
}
.message-list-card {
  margin-bottom: 20px;
}
.quick-actions-card {
  margin-bottom: 20px;
}
.action-button {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
.action-button .action-icon {
  font-size: 36px;
  margin-right: 16px;
  opacity: 0.9;
}
.action-button .action-icon.pet {
  color: #ffd700;
}
.action-button .action-icon.order {
  color: #90ee90;
}
.action-button .action-content {
  flex: 1;
}
.action-button .action-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}
.action-button .action-desc {
  font-size: 13px;
  opacity: 0.85;
}
.virtual-messages {
  list-style: none;
  padding: 0;
  margin: 0;
}
.message-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #606266;
}
.message-item:last-child {
  border-bottom: none;
}
.msg-icon {
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
}
.notify-card {
  margin-top: 8px;
}
</style>
