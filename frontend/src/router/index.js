import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requireAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/home',
    meta: { requireAuth: true },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requireAuth: true }
      },
      {
        path: 'store/list',
        name: 'StoreList',
        component: () => import('@/views/store/StoreList.vue'),
        meta: { title: '门店管理', requireAuth: true }
      },
      {
        path: 'store/user',
        name: 'StoreUser',
        component: () => import('@/views/store/StoreUser.vue'),
        meta: { title: '门店用户', requireAuth: true }
      },
      {
        path: 'report/fill',
        name: 'ReportFill',
        component: () => import('@/views/report/ReportFill.vue'),
        meta: { title: '日报填报', requireAuth: true }
      },
      {
        path: 'report/query',
        name: 'ReportQuery',
        component: () => import('@/views/report/ReportQuery.vue'),
        meta: { title: '日报查询', requireAuth: true }
      },
      {
        path: 'report/audit',
        name: 'ReportAudit',
        component: () => import('@/views/report/ReportAudit.vue'),
        meta: { title: '日报审核', requireAuth: true }
      },
      {
        path: 'report/unfilled',
        name: 'UnfilledStats',
        component: () => import('@/views/report/UnfilledStats.vue'),
        meta: { title: '未填报统计', requireAuth: true }
      },
      {
        path: 'analysis/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/analysis/Dashboard.vue'),
        meta: { title: '总部看板', requireAuth: true }
      },
      {
        path: 'analysis/daily',
        name: 'DailyReport',
        component: () => import('@/views/analysis/DailyReport.vue'),
        meta: { title: '日报分析', requireAuth: true }
      },
      {
        path: 'analysis/monthly',
        name: 'MonthlyReport',
        component: () => import('@/views/analysis/MonthlyReport.vue'),
        meta: { title: '月报分析', requireAuth: true }
      },
      {
        path: 'analysis/channel',
        name: 'ChannelAnalysis',
        component: () => import('@/views/analysis/ChannelAnalysis.vue'),
        meta: { title: '渠道分析', requireAuth: true }
      },
      {
        path: 'analysis/ranking',
        name: 'StoreRanking',
        component: () => import('@/views/analysis/StoreRanking.vue'),
        meta: { title: '门店排名', requireAuth: true }
      },
      {
        path: 'analysis/trend',
        name: 'Trend',
        component: () => import('@/views/analysis/Trend.vue'),
        meta: { title: '趋势分析', requireAuth: true }
      },
      {
        path: 'investor/list',
        name: 'InvestorList',
        component: () => import('@/views/investor/InvestorList.vue'),
        meta: { title: '投资人管理', requireAuth: true }
      },
      {
        path: 'investor/relation',
        name: 'InvestorRelation',
        component: () => import('@/views/investor/InvestorRelation.vue'),
        meta: { title: '投资关系', requireAuth: true }
      },
      {
        path: 'system/user',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', requireAuth: true }
      },
      {
        path: 'system/role',
        name: 'RoleList',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', requireAuth: true }
      },
      {
        path: 'system/menu',
        name: 'MenuList',
        component: () => import('@/views/system/MenuList.vue'),
        meta: { title: '菜单管理', requireAuth: true }
      },
      {
        path: 'system/template',
        name: 'TemplateList',
        component: () => import('@/views/system/TemplateList.vue'),
        meta: { title: '模板管理', requireAuth: true }
      },
      {
        path: 'system/field',
        name: 'FieldConfig',
        component: () => import('@/views/system/FieldConfig.vue'),
        meta: { title: '字段配置', requireAuth: true }
      },
      {
        path: 'system/config',
        name: 'ConfigList',
        component: () => import('@/views/system/ConfigList.vue'),
        meta: { title: '系统配置', requireAuth: true }
      },
      {
        path: 'system/log',
        name: 'OperationLog',
        component: () => import('@/views/system/OperationLog.vue'),
        meta: { title: '操作日志', requireAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - 林夕置业经营日报系统'
  }

  if (to.meta.requireAuth === false) {
    // 登录页：已登录则跳转首页
    if (token && to.path === '/login') {
      next('/home')
    } else {
      next()
    }
  } else if (token) {
    next()
  } else {
    next('/login')
  }
})

export default router
