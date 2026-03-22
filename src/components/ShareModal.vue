<template>
  <a-modal v-model:open="visible" :title="title" @close="closeModal">
    <h2>{{props.title}}</h2>
    <h3>链接分享</h3>
    <a-typography-link copyable>
      {{ link}}
    </a-typography-link>
    <h3>二维码分享</h3>
    <a-qrcode :value="link"/>
  </a-modal>
</template>

<script setup lang="ts">

import { ref } from 'vue'

const visible = ref<boolean>(false);

interface Props{
  title?: string,
  link?: string,
}

// 设置默认值能够直接省略 props
const props = withDefaults(defineProps<Props>(), {
  title: () => "分享",
  link: () => "https://ts1.tc.mm.bing.net/th/id/R-C.0898aacd073a4af9ca5dc25b3fedbdde?rik=Qkrj2a1BeOE%2bUQ&riu=http%3a%2f%2fn.sinaimg.cn%2fsina" +
    "cn20191001ac%2f40%2fw480h360%2f20191001%2f36f9-ifmectk3191775.jpg&ehk=nwIFF86ZAsGhHm2CAzJ1TKaWoe4IgUTBCWG4V6ZlSIw%3d&risl=&pid=ImgRaw&r=0"
})

// 关闭弹窗
const closeModal = () => {
  visible.value = false;
}

// 打开弹窗，暴露给调用者，方便展示
const openModal = () => {
  visible.value = true;
  // console.log(666)
}

defineExpose({
  openModal
})
</script>

<style scoped>

</style>
