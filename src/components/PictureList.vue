<template>
  <div class="picture-list">
    <a-list
      :grid="{ gutter: 12, xs: 1, sm: 2, md: 3, lg: 4, xxl: 6 }"
      :data-source="pictureList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item class="list-item"> <!-- 新增类名统一样式 -->
          <a-card hoverable @click="doClickPicture(picture)" class="picture-card">
            <!-- 图片容器：核心修改 -->
            <template #cover>
              <div class="image-container">
                <img
                  :alt="picture.name"
                  :src="picture.thumbnailUrl ?? picture.url"
                  class="optimized-image"
                />
              </div>
            </template>
            <a-card-meta :title="picture.name" :description="picture.introduction ?? '还没有介绍'" />
            <template #actions v-if="props.showOp">
              <a-space>
                <a-button type="primary" danger @click.stop="deleteHandler(picture, $event)">
                  <DeleteOutlined />
                </a-button>
                <a-button type="primary" @click.stop="editHandler(picture, $event)">
                  <EditOutlined />
                </a-button>
                <a-button type="dashed" @click.stop="searchHandler(picture, $event)">
                  <SearchOutlined />
                </a-button>
                <a-button type="primary" ghost @click.stop="shareHandler(picture, $event)">
                  <ShareAltOutlined />
                </a-button>
              </a-space>
            </template>
            <ShareModal ref="shareModalRef" :link="shareLink" />
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { deletePictureUsingDelete } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import {
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import ShareModal from '@/components/ShareModal.vue'
import { ref } from 'vue'

interface Props {
  pictureList: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  pictureList: () => [],
  loading: () => false,
})

const router = useRouter()
const doClickPicture = (picture: API.PictureVO) => {
  router.push(`/picture/${picture.id}`)
}

const deleteHandler = async (picture: API.Picture, e: Event) => {
  e.stopPropagation()
  const id = picture.id
  if (!id) return
  const res = await deletePictureUsingDelete({ id })
  if (res.data.code === 0 && res.data.data) {
    message.success('删除成功!')
    props.onReload?.()
  } else {
    message.error('删除失败！' + res.data.message)
  }
}

const editHandler = async (picture: API.Picture, e: Event) => {
  e.stopPropagation()
  const id = picture.id
  router.push('/add_picture?id=' + id)
}

const searchHandler = async (picture: API.Picture, e: Event) => {
  e.stopPropagation()
  const id = picture.id
  router.push('/search_picture?id=' + id)
}

const shareModalRef = ref()
const shareLink = ref<string>()
const shareHandler = (picture: API.Picture, e: Event) => {
  e.stopPropagation()
  shareLink.value = picture.url
  shareModalRef.value?.openModal()
}
</script>

<style scoped>
/* 统一列表项样式，避免边距影响 */
.list-item {
  padding: 0;
}

/* 卡片样式优化，确保高度自适应 */
.picture-card {
  height: 100%; /* 关键：让卡片撑满列表项高度 */
  display: flex;
  flex-direction: column;
}

/* 核心：固定宽高比的图片容器（4:3比例，可根据需求调整） */
.image-container {
  position: relative;
  width: 100%;
  padding-top: 75%; /* 4:3 比例 (3/4=75%)，1:1则设为100% */
  overflow: hidden;
}

/* 图片绝对定位，填充整个比例容器 */
.optimized-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持比例填充，裁剪超出部分 */
  object-position: center top; /* 初始顶部对齐 */
  transition: object-position 0.3s ease;
}

/* 悬停时居中显示 */
.optimized-image:hover {
  object-position: center center;
}

/* 卡片内容区域自动填充剩余空间 */
:deep(.ant-card-body) {
  flex: 1;
  padding: 12px;
}

/* 卡片meta样式优化，避免标题换行异常 */
:deep(.ant-card-meta) {
  width: 100%;
  overflow: hidden;
}

:deep(.ant-card-meta-title) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
