<template>
  <div class="page-shell operational-stats-page">
    <section class="page-head">
      <div>
        <span class="page-head__eyebrow">Operations Overview</span>
        <h1 class="page-head__title">门店运营概览</h1>
        <p class="page-head__desc">用更直观的方式查看疫苗记录、房态和订单状态，让日常运营压力一眼可见。</p>
      </div>
    </section>

    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>疫苗记录总数</template>
          <div class="stat-value">{{ stats.totalVaccineRecords ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>当前寄养中</template>
          <div class="stat-value">{{ stats.totalActiveBoardings ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>标准间空位</template>
          <div class="stat-value">{{ stats.roomStats?.Standard?.available ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>豪华间空位</template>
          <div class="stat-value">{{ stats.roomStats?.Deluxe?.available ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>疫苗库存统计</template>
          <el-table :data="vaccineList" style="width: 100%" max-height="320">
            <el-table-column prop="name" label="疫苗名称" />
            <el-table-column prop="count" label="记录数量" width="120">
              <template #default="scope">
                <el-tag>{{ scope.row.count }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>房间使用情况</template>
          <el-table :data="roomList" class="room-table" style="width: 100%">
            <el-table-column label="房型" width="120">
              <template #default="scope">{{ roomTypeLabel(scope.row.type) }}</template>
            </el-table-column>
            <el-table-column prop="capacity" label="总容量" width="90" />
            <el-table-column prop="used" label="已使用" width="90" />
            <el-table-column prop="available" label="剩余空位" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.available > 0 ? 'success' : 'danger'">
                  {{ scope.row.available }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="使用率" width="100">
              <template #default="scope">{{ scope.row.usageRate ?? 0 }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px;">
      <template #header>订单状态分布</template>
      <div class="status-distribution">
        <div class="status-item">
          <div class="status-label">待确认</div>
          <div class="status-count">{{ stats.statusDistribution?.CREATED ?? 0 }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">已确认</div>
          <div class="status-count">{{ stats.statusDistribution?.CONFIRMED ?? 0 }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">寄养中</div>
          <div class="status-count">{{ stats.statusDistribution?.BOARDING ?? 0 }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">已完成</div>
          <div class="status-count">{{ stats.statusDistribution?.COMPLETED ?? 0 }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">已取消</div>
          <div class="status-count">{{ stats.statusDistribution?.CANCELLED ?? 0 }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'

const stats = ref<any>({})
const loading = ref(false)

const vaccineList = computed(() => {
  const inventory = stats.value.vaccineInventory || {}
  return Object.entries(inventory).map(([name, count]) => ({ name, count }))
})

const roomList = computed(() => {
  const roomStats = stats.value.roomStats || {}
  return Object.entries(roomStats).map(([type, info]: [string, any]) => ({
    type,
    capacity: info.capacity,
    used: info.used,
    available: info.available,
    usageRate: info.usageRate
  }))
})

const roomTypeLabel = (type: string) => ({
  Standard: '标准间',
  Deluxe: '豪华间',
  Suite: '套房'
}[type] || type)

const fetchStats = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/stats/operational')
    stats.value = res.data || {}
  } catch (error) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.stat-value {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  color: #409eff;
}
.status-distribution {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 20px;
}
.status-item {
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.58);
  border-radius: 20px;
  box-shadow: inset 0 0 0 1px rgba(40, 71, 54, 0.08);
  min-width: 120px;
}
.status-label {
  font-size: 14px;
  color: var(--pet-muted);
  margin-bottom: 8px;
}
.status-count {
  font-size: 32px;
  font-weight: bold;
  color: var(--pet-forest);
}

.room-table {
  width: 100%;
}

.room-table:deep(.el-table__header),
.room-table:deep(.el-table__body),
.room-table:deep(.el-table__footer) {
  width: 100% !important;
}

.room-table:deep(.el-table__header-wrapper table),
.room-table:deep(.el-table__body-wrapper table) {
  width: 100% !important;
}
</style>
