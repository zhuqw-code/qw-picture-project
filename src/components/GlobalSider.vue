<template>
  <div id="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id"
      width="120"
      breakpoint="lg"
      collapsed-width="0"
    >
      <a-menu
        v-model:selected-keys="current"
        mode="inline"
        :style="{ height: '100%', borderRight: 0 }"
        :items="menuItems"
        @click="doMenuClick"
      >
      </a-menu>
    </a-layout-sider>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { PictureOutlined, UserOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/user_status.ts'
import { useRoute, useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore()

const menuItems = [
  {
    key: '/',
    icon: () => h(PictureOutlined),
    label: '公共图库',
  },
  {
    key: '/my_space',
    icon: () => h(UserOutlined),
    label: '我的空间',
  },
  {
    key: '/add_space',
    icon: () => h(PlusOutlined),
    label: '创建空间',
  },
]

/**
 * 路由跳转
 */
const router = useRouter()
const current = ref<string[]>(['/'])
// let pathList = menuItems.map(item => item.key);
// 根据路由修改导航栏高亮
router.afterEach((to, form, next) => {
  let target = to.path;
  if (to.path !== '/' && to.path !== '/add_space'){
    target = "/my_space";
  }
  current.value = [target];
})
// 根据导航栏修改路由页面
const doMenuClick = ({ key }: any) => {
  router.push({
    path: key,
  })
}
</script>
