<template>
  <div id="global-header">
    <!-- 通过设置 wrap即使空间不够也不能换行 -->
    <a-row :wrap="false">
      <a-col flex="250px">
        <div class="title-bar">
          <img class="logo" src="@/assets/logo.jpg" title="I AM BEAUTIFUL" />
          <div class="title" style="color: blue">智能云图库</div>
        </div>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="160px">
        <div id="user-login-status">
          <div v-if="loginUserStore.loginUser?.id">
            <a-dropdown placement="bottom" trigger="hover">
              <a-space>
                <a-avatar :size="48" :src="loginUserStore.loginUser.userAvatar ?? 'https://qw-1346071538.cos.ap-nanjing.myqcloud.com///space/6/2025-11-16_7692c8d1-13c6-4bf7-964b-013496d4b7d4.b8.png'" />
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="goUserCenter">
                    <UserOutlined />
                    个人中心
                  </a-menu-item>
                  <a-menu-item @click="userLogout">
                    <MinusCircleTwoTone />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <h3 class="userName">{{ loginUserStore?.loginUser.userName }}</h3>
          </div>
          <div v-else>
            <a-button class="btn" type="primary" href="/user/login"> 登录 </a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import {
  MinusCircleTwoTone,
  MailOutlined,
  AuditOutlined,
  PhoneOutlined,
  QuestionCircleOutlined,
  PlusOutlined, BarChartOutlined, UserOutlined
} from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import {useLoginUserStore} from '@/stores/user_status.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
// 获取到用户状态
const loginUserStore = useLoginUserStore();
loginUserStore.fetchLoginUser();       // 模拟发送请求获取用户状态
// console.log(loginUserStore.loginUser.userName)

const current = ref<string[]>([]);
const originItems = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(MailOutlined),
    label: '主页',
    title: 'main pages',
  },
  {
    key: '/admin/manager',
    icon: () => h(AuditOutlined),
    label: '用户管理',
    title: 'User Manager',
  },
  {
    key: '/admin/pictureManager',
    icon: () => h(AuditOutlined),
    label: '图片管理',
    title: 'Picture Manager',
  },
  {
    key: '/admin/spaceManager',
    icon: () => h(AuditOutlined),
    label: '用户空间',
    title: 'UserSpace Manager',
  },
  {
    key: '/add_picture',
    icon: () => h(PlusOutlined),
    label: '新增图片页',
    title: 'add picture',
  },
]);

/**
 * 根据用户角色对页面呈现信息进行动态变化
 */

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    // 管理员才能看到 /admin 页面
    if (menu?.key?.startsWith("/admin")){
      //todo 必须放到这里，因为计算属性是否执行关键就看 loginUser 状态的变化
      const loginUser = loginUserStore.loginUser;
      if (!loginUser || loginUser.userRole !== 'admin'){
        return false;
      }
    }
    return true;
  })
}

// 主要就是看用户状态是否变化
const items = computed(() => filterMenus(originItems.value));

// 点击进行路由跳转（根据要么使用侦听器，要么使用点击事件）
// 这里其实返回的是个 Event对象包含 key 属性这样编写直接结构了
const router = useRouter()
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}

// 问题：我们高亮某个页面标签，但是刷新浏览器后就会再次设置为默认值 current = null
// 解决：在页面跳转时进行设置 next指的是跳转失败后的页面
// 注意：这里的 to是个对象
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 退出登录，清除状态信息
const userLogout = async () => {
  // 后端清除用户状态
  const res = await userLogoutUsingPost(); // 向后端发送请求是异步的，使用 await后会在获取到后端响应后在进行后续代码执行（异步 -> 同步）
  if (res.data.code === 0) {
    message.success("安全退出~");
    // 前端清除全局用户信息
    loginUserStore.setLoginUser({});
    await router.push({
      path: "/user/login",  // todo 退出后跳转到登录页面，但是一般跳到首页比较合理
      replace: true,
    })
  }
  else {
    message.error("退出失败：" + res.data.message);
  }
}

// 进入个人中心
const goUserCenter = async () => {
  if (!loginUserStore.loginUser?.id) {
    await router.push({ path: '/user/login', replace: true })
    return
  }
  await router.push({ path: '/user/center' })
}


</script>


<style scoped>
#global-header {
  margin-inline: -50px;
  //background-image: linear-gradient(to top, #5ee7df 0%, #b490ca 100%);
  backdrop-filter: blur(10px);
}

.title-bar {
  display: flex;
  align-items: center;
}

.title-bar .title {
  color: #ffffff;
}

/* 让顶部菜单背景透明，避免白块突兀 */
#global-header :deep(.ant-menu),
#global-header :deep(.ant-menu-horizontal) {
  background: transparent !important;
}

.title-bar .logo {
  width: 100px;
  margin-left: 20px;
}

.title-bar .title {
  display: inline-block;
  margin-left: 16px;
  font-size: 18px;
}

#user-login-status .btn {
  vertical-align: center;
}

#user-login-status .userName{
  display: inline-block;
  margin-left: 5px;
}
</style>
