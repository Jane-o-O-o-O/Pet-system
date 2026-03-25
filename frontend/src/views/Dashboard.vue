<template>
  <div class="page-shell dashboard-page">
    <section class="dashboard-hero">
      <div class="dashboard-hero__copy">
        <span class="page-head__eyebrow">{{ dashboardProfile.eyebrow }}</span>
        <h1 class="dashboard-hero__title">{{ welcomeTitle }}，{{ userStore.username }}</h1>
        <p class="dashboard-hero__desc">{{ dashboardProfile.description }}</p>

        <div class="dashboard-hero__meta">
          <span class="metric-pill"><strong>角色</strong>{{ roleLabel }}</span>
          <span class="metric-pill"><strong>时钟</strong>{{ currentTime }}</span>
          <span class="metric-pill"><strong>状态</strong>{{ dashboardProfile.status }}</span>
        </div>
      </div>

      <div class="dashboard-hero__card">
        <span class="dashboard-hero__cap">今日焦点</span>
        <strong class="dashboard-hero__value">{{ dashboardProfile.featured.value }}</strong>
        <span class="dashboard-hero__label">{{ dashboardProfile.featured.label }}</span>
        <ul class="dashboard-hero__facts">
          <li v-for="fact in dashboardProfile.sideFacts" :key="fact.label">
            <span>{{ fact.label }}</span>
            <strong>{{ fact.value }}</strong>
          </li>
        </ul>
      </div>
    </section>

    <section class="dashboard-stat-grid">
      <article v-for="card in dashboardProfile.cards" :key="card.label" class="stat-plate">
        <span class="stat-plate__eyebrow">{{ card.eyebrow }}</span>
        <strong class="stat-plate__value">{{ card.value }}</strong>
        <h3 class="stat-plate__title">{{ card.label }}</h3>
        <p class="stat-plate__desc">{{ card.description }}</p>
      </article>
    </section>

    <section class="dashboard-bento">
      <article class="page-panel">
        <div class="section-head">
          <div>
            <p class="section-head__hint">Quick Actions</p>
            <h2 class="section-head__title">常用动作</h2>
          </div>
          <span class="section-head__hint">围绕你当前身份的快捷入口</span>
        </div>

        <div class="action-grid">
          <button
            v-for="action in dashboardProfile.actions"
            :key="action.label"
            type="button"
            class="action-tile"
            @click="goAction(action)"
          >
            <span class="action-tile__eyebrow">{{ action.kicker }}</span>
            <strong>{{ action.label }}</strong>
            <span>{{ action.description }}</span>
          </button>
        </div>
      </article>

      <article class="page-panel">
        <div class="section-head">
          <div>
            <p class="section-head__hint">Care Feed</p>
            <h2 class="section-head__title">照护播报</h2>
          </div>
          <span class="section-head__hint">把待办和提醒压缩成一眼能抓住的摘要</span>
        </div>

        <ul class="message-rail">
          <li v-for="(msg, index) in dashboardProfile.messages" :key="msg" class="message-rail__item">
            <span class="message-rail__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <p>{{ msg }}</p>
          </li>
        </ul>
      </article>
    </section>

    <section class="page-panel table-card">
      <div class="section-head">
        <div>
          <p class="section-head__hint">Live Notifications</p>
          <h2 class="section-head__title">系统通知</h2>
        </div>
        <span class="section-head__hint">真实通知为空时展示角色示例，避免界面失焦</span>
      </div>

      <el-table :data="displayNotifications" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="200" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              v-if="scope.row.readFlag === 0 && scope.row.id"
              link
              type="primary"
              @click="markAsRead(scope.row.id)"
            >
              标记已读
            </el-button>
            <span v-else-if="scope.row.id" class="notify-state">已读</span>
            <span v-else class="notify-state">示例</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > pageSize"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import request from '../utils/request'
import { formatDateTime } from '../utils/date'

type ActionItem = {
  kicker: string
  label: string
  description: string
  path: string
  query?: Record<string, string>
}

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
  const map: Record<string, string> = {
    ADMIN: '管理员',
    STAFF: '医护人员',
    OWNER: '宠物主人'
  }
  return map[userStore.role] || userStore.role
})

const welcomeTitle = computed(() => '欢迎回来')

const ownerStats = ref({ petCount: 0, vaccineRemindCount: 0, activeOrderCount: 0 })
const ownerMessages = ref([
  '您有一条疫苗即将到期提醒，请及时在「我的宠物」中查看并预约。',
  '完善宠物档案可获得更精准的寄养与健康建议。',
  '首次寄养用户建议先补全过敏史、喂养偏好和紧急联系人。'
])

const staffStats = ref({ todayCheckIn: 0, pendingConfirm: 0, boardingNow: 0 })
const staffMessages = ref([
  '今日有订单待确认，请优先联系首次寄养的主人完成交接说明。',
  '在店寄养宠物中有入住接近结束的个体，建议提前沟通续住或接回。',
  '请更新照护记录，保持喂食、清洁和活动时间线完整。'
])

const adminStats = ref({ userCount: 0, petCount: 0, orderCount: 0, todayOrders: 0 })
const adminMessages = ref([
  '本月寄养订单较上月提升，排期与房态需要同步关注。',
  '注册用户、宠物档案和订单量仍在稳步增长。',
  '运营概览无异常告警，适合进一步优化服务节奏与转化。'
])

const virtualNotificationsByRole: Record<string, any[]> = {
  OWNER: [
    { title: '疫苗提醒', content: '您的爱宠「小白」的狂犬疫苗将于 7 日后到期，请及时接种。', createdAt: new Date().toLocaleString(), readFlag: 0, id: null },
    { title: '服务说明', content: '寄养订单确认后请按约定时间送达，如有变更请提前联系门店。', createdAt: new Date().toLocaleString(), readFlag: 1, id: null }
  ],
  STAFF: [
    { title: '待办提醒', content: '订单 #BO20260225001 待确认，请今日内联系主人完成确认。', createdAt: new Date().toLocaleString(), readFlag: 0, id: null },
    { title: '交接说明', content: '新入住宠物「豆豆」已完成入住登记，请按照护理须知执行。', createdAt: new Date().toLocaleString(), readFlag: 1, id: null }
  ],
  ADMIN: [
    { title: '数据日报', content: '昨日新增用户 3 人、订单 2 笔，系统运行正常。', createdAt: new Date().toLocaleString(), readFlag: 1, id: null },
    { title: '系统公告', content: '本周六 00:00-02:00 进行例行维护，请提前通知相关用户。', createdAt: new Date().toLocaleString(), readFlag: 0, id: null }
  ]
}

const displayNotifications = computed(() => {
  const list = notifications.value.length > 0 ? notifications.value : (virtualNotificationsByRole[userStore.role] || [])
  return list.map((row: any) => ({
    ...row,
    createdAt: formatDateTime(row.createdAt || row.created_at)
  }))
})

const dashboardProfile = computed(() => {
  if (userStore.role === 'STAFF') {
    return {
      eyebrow: 'Front Desk / Staff',
      description: '面向前台与护理团队的工作台，把入住、确认、照护记录压缩成更顺手的操作面。',
      status: '门店运转中',
      featured: { value: staffStats.value.boardingNow, label: '当前在店寄养' },
      sideFacts: [
        { label: '今日入住', value: staffStats.value.todayCheckIn },
        { label: '待确认', value: staffStats.value.pendingConfirm }
      ],
      cards: [
        { eyebrow: 'Check-in', value: staffStats.value.todayCheckIn, label: '今日入住', description: '今日进入门店并完成入住登记的宠物数量。' },
        { eyebrow: 'Pending', value: staffStats.value.pendingConfirm, label: '待确认订单', description: '建议优先处理首次寄养和临近入住日期的订单。' },
        { eyebrow: 'Boarding', value: staffStats.value.boardingNow, label: '在店宠物', description: '帮助你快速掌握现场照护压力与排班节奏。' }
      ],
      actions: [
        { kicker: 'Record', label: '录入病历', description: '进入宠物管理页补充医疗与疫苗记录。', path: '/staff/pets' },
        { kicker: 'Boarding', label: '处理寄养', description: '确认、入住、完成寄养流程都在这里。', path: '/staff/orders' },
        { kicker: 'Ops', label: '查看房态', description: '快速了解库存、房型与运营占用情况。', path: '/staff/operational-stats' }
      ] satisfies ActionItem[],
      messages: staffMessages.value
    }
  }

  if (userStore.role === 'ADMIN') {
    return {
      eyebrow: 'Operations / Admin',
      description: '这是经营视角的总控面板，用更有层次的方式呈现用户、宠物和订单的流动状态。',
      status: '经营视图已同步',
      featured: { value: adminStats.value.todayOrders, label: '今日新增订单' },
      sideFacts: [
        { label: '用户总数', value: adminStats.value.userCount },
        { label: '宠物档案', value: adminStats.value.petCount }
      ],
      cards: [
        { eyebrow: 'Users', value: adminStats.value.userCount, label: '注册用户', description: '反映当前系统覆盖的客户基础与增长规模。' },
        { eyebrow: 'Pets', value: adminStats.value.petCount, label: '宠物档案', description: '完整档案越多，后续推荐与提醒越精准。' },
        { eyebrow: 'Orders', value: adminStats.value.orderCount, label: '寄养订单', description: '帮助判断营收、入住率和服务压力。' },
        { eyebrow: 'Today', value: adminStats.value.todayOrders, label: '今日订单', description: '用于判断当日流量与接待节奏是否健康。' }
      ],
      actions: [
        { kicker: 'Users', label: '管理用户', description: '调整角色、状态并创建系统用户。', path: '/admin/users' },
        { kicker: 'Reports', label: '经营报表', description: '按日期区间查看寄养表现和疫苗到期提醒。', path: '/admin/stats' },
        { kicker: 'Ops', label: '运营大盘', description: '回到门店运营侧，关注房态与寄养流程。', path: '/staff/operational-stats' }
      ] satisfies ActionItem[],
      messages: adminMessages.value
    }
  }

  return {
    eyebrow: 'Pet Owner / Archive',
    description: '从宠物档案到寄养预约，把所有日常照护的细节收进一个像日记本一样的控制台。',
    status: '档案持续完善中',
    featured: { value: ownerStats.value.petCount, label: '已建档宠物' },
    sideFacts: [
      { label: '疫苗提醒', value: ownerStats.value.vaccineRemindCount },
      { label: '进行中订单', value: ownerStats.value.activeOrderCount }
    ],
    cards: [
      { eyebrow: 'Archive', value: ownerStats.value.petCount, label: '我的宠物', description: '基础信息、体重、绝育与备注统一管理。' },
      { eyebrow: 'Health', value: ownerStats.value.vaccineRemindCount, label: '待接种提醒', description: '把重要时间点提前暴露，减少遗漏。' },
      { eyebrow: 'Boarding', value: ownerStats.value.activeOrderCount, label: '进行中订单', description: '快速追踪当前寄养安排与订单状态。' }
    ],
    actions: [
      { kicker: 'Create', label: '新增宠物', description: '马上建立新的宠物档案。', path: '/owner/pets', query: { action: 'add' } },
      { kicker: 'Reserve', label: '预约寄养', description: '为毛孩子安排下一次入住。', path: '/owner/orders', query: { action: 'add' } },
      { kicker: 'Clinic', label: '挂号预约', description: '提交到诊时间和症状，等待医护确认。', path: '/owner/appointments', query: { action: 'add' } },
      { kicker: 'Orders', label: '查看订单', description: '了解当前与历史寄养记录。', path: '/owner/orders' }
    ] satisfies ActionItem[],
    messages: ownerMessages.value
  }
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

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
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

const fetchOverview = async () => {
  try {
    const res: any = await request.get('/dashboard/overview')
    const data = res.data || {}

    if (userStore.role === 'OWNER') {
      ownerStats.value = {
        petCount: data.petCount ?? 0,
        vaccineRemindCount: data.vaccineRemindCount ?? 0,
        activeOrderCount: data.activeOrderCount ?? 0
      }
    } else if (userStore.role === 'STAFF') {
      staffStats.value = {
        todayCheckIn: data.todayCheckIn ?? 0,
        pendingConfirm: data.pendingConfirm ?? 0,
        boardingNow: data.boardingNow ?? 0
      }
    } else if (userStore.role === 'ADMIN') {
      adminStats.value = {
        userCount: data.users ?? 0,
        petCount: data.pets ?? 0,
        orderCount: data.orders ?? 0,
        todayOrders: data.todayOrders ?? 0
      }
    }
  } catch (error) {
    // keep default values
  }
}

const goAction = (action: ActionItem) => {
  router.push({ path: action.path, query: action.query })
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
.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(300px, 0.7fr);
  gap: 22px;
}

.dashboard-hero__copy,
.dashboard-hero__card,
.stat-plate {
  position: relative;
  overflow: hidden;
  border-radius: 34px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  box-shadow: 0 24px 70px rgba(49, 35, 20, 0.1);
}

.dashboard-hero__copy {
  padding: 34px;
  background:
    linear-gradient(140deg, rgba(255, 250, 241, 0.94), rgba(255, 243, 223, 0.82)),
    radial-gradient(circle at 90% 10%, rgba(231, 159, 97, 0.18), transparent 20%);
}

.dashboard-hero__copy::after {
  content: "";
  position: absolute;
  right: 36px;
  top: 26px;
  width: 136px;
  height: 72px;
  background:
    radial-gradient(circle, rgba(40, 71, 54, 0.12) 0 16%, transparent 17%) 18px 4px / 52px 52px,
    radial-gradient(circle, rgba(40, 71, 54, 0.12) 0 8%, transparent 9%) 0 34px / 52px 52px,
    radial-gradient(circle, rgba(40, 71, 54, 0.12) 0 8%, transparent 9%) 34px 34px / 52px 52px,
    radial-gradient(circle, rgba(40, 71, 54, 0.12) 0 8%, transparent 9%) 68px 34px / 52px 52px;
  opacity: 0.7;
}

.dashboard-hero__title {
  margin: 16px 0 12px;
  font-family: var(--font-display);
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.04;
}

.dashboard-hero__desc {
  max-width: 720px;
  margin: 0;
  line-height: 1.8;
  color: var(--pet-muted);
}

.dashboard-hero__meta {
  margin-top: 26px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.dashboard-hero__card {
  padding: 30px;
  background:
    linear-gradient(180deg, rgba(29, 53, 39, 0.95), rgba(20, 35, 26, 0.98)),
    radial-gradient(circle at top right, rgba(231, 159, 97, 0.26), transparent 24%);
  color: rgba(255, 248, 235, 0.94);
}

.dashboard-hero__card::after {
  content: "";
  position: absolute;
  right: -24px;
  bottom: -24px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(231, 159, 97, 0.22), transparent 68%);
}

.dashboard-hero__cap {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.dashboard-hero__value {
  display: block;
  margin-top: 26px;
  font-family: var(--font-display);
  font-size: clamp(58px, 8vw, 88px);
  line-height: 0.95;
}

.dashboard-hero__label {
  display: block;
  margin-top: 8px;
  color: rgba(255, 248, 235, 0.76);
}

.dashboard-hero__facts {
  list-style: none;
  padding: 0;
  margin: 28px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.dashboard-hero__facts li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.dashboard-hero__facts span {
  color: rgba(255, 248, 235, 0.68);
}

.dashboard-stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.stat-plate {
  padding: 24px;
  background: rgba(255, 250, 241, 0.84);
}

.stat-plate::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent 45%),
    radial-gradient(circle at 100% 0, rgba(231, 159, 97, 0.08), transparent 24%);
  pointer-events: none;
}

.stat-plate__eyebrow {
  position: relative;
  z-index: 1;
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(40, 71, 54, 0.06);
  color: var(--pet-forest);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.stat-plate__value {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 24px;
  font-family: var(--font-display);
  font-size: clamp(40px, 5vw, 56px);
  line-height: 1;
}

.stat-plate__title {
  position: relative;
  z-index: 1;
  margin: 14px 0 8px;
  font-size: 22px;
}

.stat-plate__desc {
  position: relative;
  z-index: 1;
  margin: 0;
  color: var(--pet-muted);
  line-height: 1.7;
}

.dashboard-bento {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.92fr);
  gap: 22px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.action-tile {
  border: 0;
  padding: 20px;
  border-radius: 24px;
  text-align: left;
  cursor: pointer;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(255, 247, 235, 0.86)),
    radial-gradient(circle at bottom right, rgba(231, 159, 97, 0.14), transparent 24%);
  box-shadow: inset 0 0 0 1px rgba(40, 71, 54, 0.08);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.action-tile:hover {
  transform: translateY(-4px);
  box-shadow: inset 0 0 0 1px rgba(40, 71, 54, 0.08), 0 18px 30px rgba(49, 35, 20, 0.08);
}

.action-tile__eyebrow {
  display: block;
  color: var(--pet-forest);
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.action-tile strong {
  display: block;
  margin: 18px 0 10px;
  font-size: 22px;
}

.action-tile span:last-child {
  color: var(--pet-muted);
  line-height: 1.7;
}

.message-rail {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-rail__item {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.52);
  box-shadow: inset 0 0 0 1px rgba(40, 71, 54, 0.06);
}

.message-rail__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--pet-forest), var(--pet-forest-deep));
  color: #fff9f0;
  font-family: var(--font-display);
  font-size: 24px;
}

.message-rail__item p {
  margin: 0;
  line-height: 1.85;
  color: var(--pet-ink);
}

.notify-state {
  color: var(--pet-muted);
}

@media (max-width: 1080px) {
  .dashboard-hero,
  .dashboard-bento {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .dashboard-hero__copy,
  .dashboard-hero__card,
  .stat-plate {
    border-radius: 26px;
  }

  .dashboard-hero__copy,
  .dashboard-hero__card,
  .stat-plate {
    padding: 22px;
  }

  .message-rail__item {
    grid-template-columns: 52px minmax(0, 1fr);
  }
}
</style>
