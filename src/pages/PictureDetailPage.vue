<template>
  <div id="detail-picture">
    <span class="title">图片详情页</span>
    <a-row :gutter="[16, 16]" style="padding: 20px 0">
      <a-col :sm="24" :xxl="16" style="text-align: center">
        <a-image :src="picture?.url" style="max-height: 600px"></a-image>
      </a-col>
      <a-col :sm="24" :xxl="8" style="text-align: center">
        <a-card
          title="Card title"
          :bordered="false"
          style="min-width: 400px; height: 100%; background-color: pink"
        >
          <!-- 图片信息区域 -->
          <a-descriptions :column="1">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="24" :src="picture.userVO?.userAvatar" />
                <a-tag color="pink">{{ picture.userVO?.userName }}</a-tag>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ picture.name ?? '未命名' }}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{ picture.introduction ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{ picture.category ?? '默认' }}
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{ picture.picFormat ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{ picture.picWidth ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{ picture.picHeight ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{ picture.picScale ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
          </a-descriptions>
          <ShareModal ref="shareModalRef" :link="picture.url" title="分享宝藏壁纸"/>
          <a-space v-if="belong" size="large">
            <a-button type="primary" @click="doEdit">编辑</a-button>
            <a-button type="primary" danger @click="doDelete">删除</a-button>
            <a-button type="primary" ghost @click="shareHandler(picture as any, $event)">分享</a-button>
          </a-space>
          <a-button @click="doDownLoad" style="width: 80%; margin: 20px 0" type="primary" size="large"
            >免费下载</a-button
          >
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { deletePictureUsingDelete, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { downloadImage, formatSize } from '../utils/tools.ts'
import { useLoginUserStore } from '@/stores/user_status.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'

// 单张图片详情
const picture = ref<API.PictureVO>({})

// 定义获取路径上的id
interface Props {
  id: string | number
}

const props = defineProps<Props>()
const id = props.id
const getPictureVO = async () => {
  // 是否要将旧页面加载出来，判断是不是更新页面，判断是否有id
  const res = await getPictureVoByIdUsingGet({ id })
  {
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      console.log(picture)
    }
  }
}

// 判断是否能够编辑该图片
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser
const belong = ref<boolean>(false)
const validated = () => {
  // 判断图片是否能够被当前人删除
  const needUserId = picture.value.userId
  const userId = loginUser.id
  const userRole = loginUser.userRole
  if (needUserId === userId || userRole === 'admin') {
    belong.value = true
  }
}
// 如果是更新就要渲染数据
onMounted(() => {
  getPictureVO()
  validated()
})

const router = useRouter()
const doEdit = () => {
  router.push('/add_picture?id=' + props.id) // 为啥传对象，不能再后面添加 query参数
}
const doDelete = async () => {
  const res = await deletePictureUsingDelete({ id })
  if (res.data.code == 0 && res.data.data) {
    message.success('删除成功' + res.data.data)
    router.push('/')
  } else {
    message.error('删除失败' + res.data.message)
  }
}

// 下载图片
const doDownLoad = () => {
  downloadImage(picture.value.url);
}


//----------------- 分享图片
const shareModalRef = ref()
const shareLink = ref<string>()
const shareHandler = (picture: API.Picture, e: Event) => {
  // 阻止事件冒泡
  e.stopPropagation()
  // 将图片路径传递给展示分享的弹窗中
  shareLink.value = picture.url
  // shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`;
  if (shareModalRef.value) {
    shareModalRef.value.openModal() // 因为调用弹窗的组件需要修改子组件中的 visiable 属性为 true才能显示弹窗，直接使用子组件 暴露出来的方法
  }
}
</script>

<style scoped>
#add-picture {
  max-width: 720px;
  margin: 0 auto;
}
#detail-picture .title {
  display: block;
  padding: 20px;
  margin: 0 auto;
  text-align: center;
  font-size: 24px;
}
</style>
