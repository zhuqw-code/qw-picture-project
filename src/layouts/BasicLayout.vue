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
import contentBgImg from "@/assets/frontend2.jpg"
import HomeView from '@/views/HomeView.vue'
import GlobalSider from '@/components/GlobalSider.vue' // 导入内容区域背景图

// 内容区域背景样式
const contentBackgroundStyle = computed(() => ({
  backgroundImage: `url(${contentBgImg})`,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  backgroundRepeat: 'no-repeat',
  minHeight: 'calc(100vh - 64px - 48px)', // 减去头部和页脚高度
}));

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
  background-color: pink;
  font-size: 20px;
}
</style>
