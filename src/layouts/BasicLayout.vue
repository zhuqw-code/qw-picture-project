<template>
  <div id="basicLayout">
    <a-layout style="min-width: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout>
        <GlobalSider />
        <a-layout-content class="content" :style="contentBackgroundStyle">
          <RouterView />
        </a-layout-content>
      </a-layout>
      <a-layout-footer class="footer">
        <a href="#">版权@zqw</a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import { computed } from 'vue'
import HomeView from '@/views/HomeView.vue'
import GlobalSider from '@/components/GlobalSider.vue' // 导入内容区域背景图
import { useRoute } from 'vue-router'

const route = useRoute()

// 内容区域背景样式
const contentBackgroundStyle = computed(() => ({
  // 个人中心页本身已经设置了自己的背景渐变，这里不再叠加全局背景。
  ...(route.path === '/user/center'
    ? { backgroundImage: 'none' }
    : {
        backgroundImage:
          'linear-gradient(60deg, #3d3393 0%, #2b76b9 37%, #2cacd1 65%, #35eb93 100%)',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
      }),
  minHeight: 'calc(100vh - 64px - 48px)', // 减去头部和页脚高度
}))

</script>

<style scoped>
#basicLayout .header {
  margin-bottom: 3px;
  background-color: white;
}

#basicLayout .content {
  margin-bottom: 64px;
  padding-left: 24px;
  padding-right: 24px;
  padding-top: 16px;
}

#basicLayout .footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  //background-image: linear-gradient(120deg, #a1c4fd 0%, #c2e9fb 100%);
  font-size: 20px;
}
</style>
