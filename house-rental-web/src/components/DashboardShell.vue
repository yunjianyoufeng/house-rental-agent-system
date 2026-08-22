<template>
  <el-container class="dashboard-shell">
    <el-aside v-if="!isMobile" width="220px" class="dashboard-aside">
      <div class="brand">
        <div class="brand-title">房屋租赁系统</div>
        <div class="brand-role">{{ role }}</div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="dashboard-menu"
        @select="handleSelect"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.key"
          :index="item.key"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-drawer
      v-model="mobileMenuVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="dashboard-mobile-drawer"
    >
      <div class="brand mobile-brand">
        <div class="brand-title">房屋租赁系统</div>
        <div class="brand-role">{{ role }}</div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="dashboard-menu mobile-menu"
        @select="handleSelect"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.key"
          :index="item.key"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-container>
      <el-header class="dashboard-header">
        <div class="dashboard-header-left">
          <el-button
            v-if="isMobile"
            circle
            class="menu-trigger"
            @click="mobileMenuVisible = true"
          >
            <el-icon><Menu /></el-icon>
          </el-button>

          <div class="dashboard-heading">
            <h2 class="dashboard-title">{{ title }}</h2>
            <div class="dashboard-subtitle">当前角色：{{ role }}</div>
          </div>
        </div>

        <div class="dashboard-actions">
          <slot name="header-actions" />
          <el-button @click="$router.push('/houses')">查看房源列表</el-button>
          <el-button type="danger" @click="$emit('logout')">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="dashboard-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Menu } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '',
  },
  role: {
    type: String,
    default: '',
  },
  menus: {
    type: Array,
    default: () => [],
  },
  activeMenu: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['menu-click', 'logout'])

const isMobile = ref(false)
const mobileMenuVisible = ref(false)

const updateViewport = () => {
  isMobile.value = window.innerWidth < 768
}

const handleSelect = (key) => {
  emit('menu-click', key)
  if (isMobile.value) {
    mobileMenuVisible.value = false
  }
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  background: transparent;
}

.dashboard-aside {
  background: linear-gradient(180deg, #0f172a 0%, #18233a 100%);
  color: #fff;
  padding: 20px 0;
  box-shadow: 12px 0 30px rgba(15, 23, 42, 0.16);
}

.brand {
  padding: 0 20px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  margin-bottom: 12px;
}

.mobile-brand {
  margin-top: 8px;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.brand-role {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
}

.dashboard-menu {
  border-right: none;
  background: transparent;
}

.dashboard-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.88);
  margin: 6px 12px;
  border-radius: 10px;
}

.dashboard-menu :deep(.el-menu-item.is-active) {
  background: #409eff;
  color: #fff;
}

.dashboard-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.dashboard-mobile-drawer :deep(.el-drawer) {
  background: #1f2937;
}

.dashboard-mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
}

.dashboard-header {
  height: auto;
  min-height: 88px;
  padding: 24px 24px 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  background: transparent;
}

.dashboard-header-left,
.dashboard-actions {
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 20px;
  padding: 16px 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.dashboard-header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.menu-trigger {
  margin-top: 2px;
  flex-shrink: 0;
}

.dashboard-heading {
  min-width: 0;
}

.dashboard-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  word-break: break-word;
}

.dashboard-subtitle {
  margin-top: 8px;
  color: #606266;
  font-size: 16px;
  word-break: break-word;
}

.dashboard-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  min-height: 72px;
}

.dashboard-main {
  padding: 24px;
}

@media (max-width: 767px) {
  .dashboard-header {
    padding: 16px 16px 0;
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-title {
    font-size: 22px;
  }

  .dashboard-subtitle {
    font-size: 14px;
  }

  .dashboard-actions {
    justify-content: flex-start;
    min-height: unset;
  }

  .dashboard-actions :deep(.el-button) {
    margin-left: 0;
  }

  .dashboard-main {
    padding: 16px;
  }
}
</style>
