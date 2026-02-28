<template>
  <div class="quick-actions">
    <el-page-header @back="$router.back()" title="返回" />
    
    <div class="actions-container">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover" class="action-card">
            <template #header>
              <div class="card-header-title">新增宠物</div>
            </template>
            <div class="action-description">
              <p>添加您的爱宠档案，记录宠物的基本信息、疫苗和医疗记录</p>
            </div>
            <el-button type="primary" size="large" @click="navigateToPets" style="width: 100%; margin-top: 16px;">
              <el-icon style="margin-right: 8px;"><Plus /></el-icon>
              立即添加
            </el-button>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card shadow="hover" class="action-card">
            <template #header>
              <div class="card-header-title">申请寄养</div>
            </template>
            <div class="action-description">
              <p>为您的爱宠预留寄养服务，选择房型、日期和服务，一键提交申请</p>
            </div>
            <el-button type="success" size="large" @click="navigateToOrders" style="width: 100%; margin-top: 16px;">
              <el-icon style="margin-right: 8px;"><Calendar /></el-icon>
              立即申请
            </el-button>
          </el-card>
        </el-col>
      </el-row>

      <el-divider></el-divider>

      <div class="tips-section">
        <h3>💡 温馨提示</h3>
        <ul class="tips-list">
          <li>首次使用前，请先完善您的宠物档案信息，这样我们能更好地照顾您的爱宠</li>
          <li>申请寄养前，请确保您的爱宠已接种必要的疫苗，保障宠物和他人的健康</li>
          <li>提交申请后，我们的工作人员会尽快与您联系以确认寄养细节</li>
          <li>有任何疑问，欢迎通过「联系我们」或在线客服与我们沟通</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Plus, Calendar } from '@element-plus/icons-vue'

const router = useRouter()

const navigateToPets = () => {
  router.push('/owner/pets')
}

const navigateToOrders = () => {
  router.push('/owner/orders')
}
</script>

<style scoped>
.quick-actions {
  padding: 20px;
}

.actions-container {
  margin-top: 20px;
}

.action-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.action-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 12px 0;
  flex: 1;
}

.tips-section {
  background-color: #f5f7fa;
  border-left: 4px solid #409eff;
  padding: 16px;
  border-radius: 4px;
  margin-top: 20px;
}

.tips-section h3 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 15px;
}

.tips-list {
  margin: 0;
  padding-left: 24px;
  color: #606266;
  font-size: 13px;
}

.tips-list li {
  margin-bottom: 8px;
  line-height: 1.6;
}

.tips-list li:last-child {
  margin-bottom: 0;
}
</style>
