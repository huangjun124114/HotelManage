<template>
  <el-container class="layout-container">
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && !isCollapse" class="mobile-overlay" @click="isCollapse = true"></div>

    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '0px' : (isMobile ? '220px' : (isCollapse ? '64px' : '220px'))" :class="['sidebar', { 'sidebar-mobile': isMobile, 'sidebar-open': isMobile && !isCollapse }]">
      <div class="logo" @click="goHome">
        <span v-if="!isCollapse || isMobile" class="logo-text">林夕置业</span>
        <span v-else class="logo-mini">林</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse && !isMobile"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#fff"
        router
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <template v-for="menu in menuList" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.id"
              :index="child.path"
            >
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon v-if="isMobile" class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-icon v-else class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <span class="system-title">林夕置业经营日报系统</span>
        </div>
        <div class="header-right">
          <a href="/manual.html" target="_blank" class="manual-link">操作手册</a>
          <MessageBell />
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserMenus } from '@/api/menu'
import MessageBell from '@/components/MessageBell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const menuList = ref([])
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) isCollapse.value = true
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const activeMenu = computed(() => route.path)

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

function goHome() {
  router.push('/home')
}

function handleMenuSelect(index) {
  router.push(index)
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}

// 将后端菜单格式转换为前端格式
function mapMenu(item) {
  return {
    id: item.id,
    name: item.menuName || item.name,
    path: item.path,
    icon: item.icon || '',
    children: item.children ? item.children.map(mapMenu) : []
  }
}

// 加载菜单
async function loadMenus() {
  try {
    const res = await getUserMenus()
    const data = res.data || []
    menuList.value = data.map(mapMenu)
  } catch (e) {
    // 如果获取菜单失败，使用默认菜单
    menuList.value = getDefaultMenus()
  }
}

// 获取用户角色
function getUserRoles() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.roles || []
  } catch {
    return []
  }
}

// 判断是否为管理角色
function isAdminRole() {
  return getUserRoles().some(r => r === 'ROLE_SUPER_ADMIN' || r === 'ROLE_CEO')
}

// 菜单降级：API失败时仅显示基础菜单，不暴露管理菜单
function getDefaultMenus() {
  const admin = isAdminRole()
  const menus = [
    { id: 1, name: '工作台', path: '/home', icon: 'HomeFilled' },
    {
      id: 3, name: '日报管理', path: '/report', icon: 'Document', children: [
        { id: 31, name: '日报管理', path: '/report/manage' },
        { id: 33, name: '日报审核', path: '/report/audit' },
        { id: 34, name: '未填报统计', path: '/report/unfilled' }
      ]
    },
    {
      id: 4, name: '经营分析', path: '/analysis', icon: 'DataAnalysis', children: [
        { id: 41, name: '总部看板', path: '/analysis/dashboard' }
      ]
    }
  ]
  // 只有管理员角色在API失败时才显示管理菜单（作为降级兜底）
  if (admin) {
    menus.push(
      {
        id: 2, name: '门店管理', path: '/store', icon: 'Shop', children: [
          { id: 21, name: '门店列表', path: '/store/list' }
        ]
      },
      {
        id: 5, name: '投资人管理', path: '/investor', icon: 'User', children: [
          { id: 51, name: '投资人列表', path: '/investor/list' }
        ]
      },
      {
        id: 6, name: '系统管理', path: '/system', icon: 'Setting', children: [
          { id: 61, name: '用户管理', path: '/system/user' },
          { id: 62, name: '角色管理', path: '/system/role' },
          { id: 63, name: '菜单管理', path: '/system/menu' },
          { id: 64, name: '模板管理', path: '/system/template' },
          { id: 65, name: '字段配置', path: '/system/field' },
          { id: 66, name: '操作日志', path: '/system/log' }
        ]
      }
    )
  }
  return menus
}

loadMenus()
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s;
}

.sidebar-menu {
  border-right: none;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #ffffff1a;
}

.logo-text {
  white-space: nowrap;
}

.logo-mini {
  font-size: 22px;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.collapse-btn:hover {
  color: #1890ff;
}

.system-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #333;
}

.username {
  font-size: 14px;
}

.manual-link {
  font-size: 13px;
  color: #666;
  text-decoration: none;
  margin-right: 8px;
  transition: color 0.2s;
}
.manual-link:hover {
  color: #1890ff;
}

.main-content {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    width: 0 !important;
    transition: width 0.3s;
    overflow: hidden;
  }
  .sidebar-open {
    width: 220px !important;
  }
  .mobile-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 999;
  }
  .system-title {
    display: none;
  }
  .main-content {
    padding: 12px;
  }
  .manual-link {
    display: none;
  }
}
</style>
