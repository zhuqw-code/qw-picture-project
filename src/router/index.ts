import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import SpaceManagerPage from '@/pages/admin/SpaceManagerPage.vue'
import PictureManagerPage from '@/pages/admin/PictureManagerPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home page',
      component: HomePage,
    },
    {
      path: '/add_picture',
      name: '新增图片页',
      component: () => import("@/pages/AddPicturePage.vue"),
    },
    {
      path: '/user/login',
      name: '用户登录页',
      component: () => import("@/pages/user/UserLoginPage.vue"),
    },
    {
      path: '/user/center',
      name: '个人中心',
      component: () => import('@/pages/user/UserCenterPage.vue'),
    },
    {
      path: '/user/register',
      name: '用户注册页',
      component: () => import("@/pages/user/UserRegisterPage.vue"),
    },
    {
      path: '/picture/:id',
      name: '图片详情',
      props: true,      // 这个很重要，会将id传递
      component: () => import("@/pages/PictureDetailPage.vue"),
    },
    {
      path: '/admin/manager',
      name: '用户管理页面',
      component: () => import("@/pages/admin/UserManagerPage.vue"),
    },
    {
      path: '/admin/pictureManager',
      name: '图片管理页面',
      component: PictureManagerPage,
    },
    {
      path: '/admin/spaceManager',
      name: '空间管理页面',
      component: SpaceManagerPage,
    },
    {
      path: '/add_space',
      name: '创建空间',
      component: () => import("@/pages/AddSpacePage.vue"),
    },
    {
      path: '/my_space',
      name: '空间详情',
      component: () => import("@/pages/MySpacePage.vue"),
    },
    {
      path: '/space/:spaceId',
      name: '空间内容展示',
      component: () => import("@/pages/SpaceDetailPage.vue"),
      props: true,
    },
    {
      path: '/space/analyze',
      name: '空间分析展示',
      component: () => import("@/pages/SpaceAnalyzePage.vue"),
      props: true,
    },
    {
      path: '/search_picture',
      name: '图片搜索',
      component: () => import("@/pages/SearchPicturePage.vue"),
    },
    {
      path: '/picture/batch',
      name: '批量添加图片',
      component: () => import("@/pages/admin/AddPictureBatchPage.vue"),
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/call',
      name: 'call us',
      component: () => import('../views/CallView.vue'),
    }
  ],
})

export default router
