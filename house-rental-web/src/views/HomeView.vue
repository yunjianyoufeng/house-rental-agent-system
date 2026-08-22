<template>
  <div class="home-page">
    <div class="hero-card">
      <div class="hero-left">
        <div class="hero-badge">在线租房平台</div>
        <h1>房屋租赁系统</h1>
        <p>快速找房、在线预约、提交申请、查看合同与订单，一站式完成租房流程。</p>

        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/login')">进入登录页</el-button>
          <el-button type="success" size="large" @click="$router.push('/register')">进入注册页</el-button>
          <el-button size="large" @click="$router.push('/houses')">查看房源列表</el-button>
        </div>
      </div>

      <div class="hero-right">
        <div class="hero-stat">
          <div class="hero-stat-label">热门城市</div>
          <div class="hero-stat-value">北京</div>
        </div>
        <div class="hero-stat">
          <div class="hero-stat-label">推荐流程</div>
          <div class="hero-stat-value">看房 → 申请 → 签约</div>
        </div>
      </div>
    </div>

    <el-card class="section-card notice-card" v-loading="loading">
      <template #header>
        <div class="section-header">
          <div>
            <h3 class="section-title">最新公告</h3>
            <div class="sub-title">系统最新通知与租房提醒</div>
          </div>
          <el-button link @click="$router.push('/houses')">去看房源</el-button>
        </div>
      </template>

      <el-empty v-if="noticeList.length === 0" description="暂无公告" />

      <div v-else class="notice-list">
        <div class="notice-item" v-for="item in noticeList" :key="item.id">
          <div class="notice-item-top">
            <div class="notice-title">{{ item.title }}</div>
            <div class="notice-time">{{ item.createTime }}</div>
          </div>
          <div class="notice-content">{{ item.content }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getNoticeListApi } from '../api/notice'

const loading = ref(false)
const noticeList = ref([])

const loadNoticeList = async () => {
  loading.value = true
  try {
    const res = await getNoticeListApi()
    noticeList.value = (res.data || []).slice(0, 5)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadNoticeList()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(180deg, #f8fbff 0%, #f5f7fa 100%);
}

.hero-card {
  max-width: 1180px;
  margin: 0 auto 24px;
  padding: 36px;
  border-radius: 24px;
  background: linear-gradient(135deg, #409eff 0%, #7c9cff 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  box-shadow: 0 18px 36px rgba(64, 158, 255, 0.25);
}

.hero-left {
  flex: 1;
}

.hero-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  margin-bottom: 14px;
  font-size: 14px;
}

.hero-left h1 {
  margin: 0 0 14px;
  font-size: 42px;
}

.hero-left p {
  margin: 0;
  line-height: 1.8;
  max-width: 620px;
  color: rgba(255, 255, 255, 0.92);
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.hero-actions :deep(.el-button) {
  min-width: 136px;
}

.hero-right {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-stat {
  padding: 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.15);
}

.hero-stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.hero-stat-value {
  font-size: 22px;
  font-weight: 700;
}

.notice-card {
  max-width: 1180px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-item {
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  background: #fff;
  transition: all 0.2s;
}

.notice-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.notice-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}

.notice-title {
  font-size: 18px;
  font-weight: 700;
}

.notice-content {
  color: #606266;
  line-height: 1.8;
}

.notice-time {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
}
</style>
