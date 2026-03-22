<template>
  <div id="search-picture">
    <h2>搜图页面</h2>
    <a-space align="start" direction="vertical">
      <h3>原图信息</h3>
      <a-image :height="300" :src="picture.url" />
    </a-space>
    <div style="margin-bottom: 8px"></div>
    <a-card>
      <a-list
        :grid="{ gutter: 12, xs: 1, sm: 2, md: 3, lg: 4, xxl: 6 }"
        :data-source="searchData"
        :loading="loading"
      >
        <!--      :pagination="pagination" 图片列表组件只负责进行数据展示不做其他业务，所以我们定义在外面，方便直接使用分页参数-->
        <template #renderItem="{ item }">
          <a-list-item>
            <a :href="item.objUrl" target="_blank">
              <a-card hoverable>
                <template #cover class="image-container">
                  <img
                    :src="item.thumbUrl"
                    style="height: 180px; object-fit: cover"
                    class="optimized-image"
                  />
                </template>
              </a-card>
            </a>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup lang="ts">
// 获取图片信息
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  getPictureVoByIdUsingGet,
  searchPictureByPictureUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons-vue'

const picture = ref<API.PictureVO>({})
const route = useRoute()
const searchData = ref<API.ImageSearchResult[]>([])
const pictureId = computed(() => {
  return route.query?.id
})
const loading = ref<boolean>(true)

const getOldPicture = async () => {
  const id = route.query?.id
  const res = await getPictureVoByIdUsingGet({ id } as any)
  if (res.data.code === 0 && res.data.data) {
    picture.value = res.data.data
    // message.success("获取成功");
  } else {
    message.error('获取失败：' + res.data.message)
  }
}

const doSearchData = async () => {
  loading.value = true
  let id = route.query?.id
  const res = await searchPictureByPictureUsingPost({ pictureId: id } as any)
  if (res.data.code === 0 && res.data.data) {
    searchData.value = res.data.data
    message.success('获取成功')
  } else {
    message.error('获取失败')
  }
  loading.value = false
}

onMounted(() => {
  getOldPicture()
})

onMounted(() => {
  doSearchData()
})
</script>

<style scoped>
.image-container {
  height: 180px;
  overflow: hidden; /* 隐藏超出部分 */
  display: flex;
  align-items: flex-start; /* 图片顶部对齐 */
}

.optimized-image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持比例填充容器 */
  object-position: center top; /* 关键：图片顶部对齐 */
  transition: object-position 0.3s ease; /* 平滑过渡 */
}

/* 悬停时显示更多内容 */
.optimized-image:hover {
  object-position: center center;
}
</style>
