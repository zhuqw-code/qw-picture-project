<template>
  <div class="page-home">
    <!--搜索栏-->
    <PictureSearchForm :on-search="searchCallback" />
<!--    <a-input-search-->
<!--      v-model:value="searchParams.searchText"-->
<!--      enter-button="搜索"-->
<!--      size="large"-->
<!--      placeholder="输入查询信息"-->
<!--      @change="doSearch"-->
<!--      style="padding: 10px 800px 20px 0px"-->
<!--    />-->
    <!--分类列表-->
    <a-tabs v-model:activeKey="selectedCategory" @change="doSearch">
      <a-tab-pane tab="全部" key="all"></a-tab-pane>
      <a-tab-pane v-for="category in categoryList" :tab="category" :key="category"></a-tab-pane>
    </a-tabs>
    <!--标签列表-->
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagsList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          <a-tag color="blue">{{ tag }}</a-tag>
        </a-checkable-tag>
      </a-space>
    </div>
    <!--图片列表-->
    <PictureList :picture-list="pictureList" :loading="loading" :show-op="false"/>
    <!--分页栏-->
    <a-pagination
      v-model:current="searchParams.current"
      :total="total"
      show-less-items
      @change="onPageChange"
      style="text-align: right; padding-bottom: 16px;"
    />
  </div>
</template>

<script setup lang="ts">
// 获取到 pictureVO 列表
// 编写请求函数，向后端获取图片信息
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'

const pictureList = ref<API.PictureVO[]>([])
const loading = ref(false)

const total = ref(0)
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

/**
 * 获取图片信息
 */
const fetchData = async () => {
  loading.value = true
  const params = {
    ...searchParams.value,
    tags: [] as string[],
  }
  // 分类查询
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // 标签
  selectedTagList.value.forEach((useTag, idx) => {
    if (useTag) {
      params.tags.push(tagsList.value[idx])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code == 0 && res.data.data) {
    pictureList.value = res.data.data.records ?? []
    total.value = parseInt(res.data.data.total as any)
    // console.log(pictureList)
  } else {
    message.error('加载失败：' + res.data.message)
  }
  loading.value = false
}

// 页面加载时获取
onMounted(() => {
  fetchData()
})

const onPageChange = (current: number, pageSize: number) => {
  searchParams.value.current = current;
  searchParams.value.pageSize = 12;
  fetchData();
}
// 分页查询
// 因为使用计算属性，改变时会被 @chang 捕获到执行 doTableChange 发送请求获取最新数据
// const pagination = computed(() => {
//   return {
//     current: searchParams.current,
//     pageSize: searchParams.pageSize,
//     total: total.value,
//     showSizeChanger: true,
//     onChange: (page: number, pageSize: number) => {
//       searchParams.current = page
//       searchParams.pageSize = pageSize
//       fetchData()
//     },
//   }
// })

// 点击图片跳转到详情页
// const router = useRouter()
// const doClickPicture = (picture: API.PictureVO) => {
//   router.push(`/picture/${picture.id}`)
// }


//// 获取标签
const categoryList = ref<string[]>([])
const tagsList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const selectedTagList = ref<boolean[]>([])
onMounted(async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    categoryList.value = res.data.data.categoryList ?? []
    tagsList.value = res.data.data.tagList ?? []
  } else {
    message.error('获取分类标签失败！')
  }
  // console.log("res" + res);
  // console.log("category" + res.data.data.categoryList);
  // console.log("tags" + res.data.data.tagList);
})

const doSearch = () => {
  searchParams.value.current = 1
  fetchData()
}

// 回调函数
const searchCallback = (newSearchParams: API.PictureQueryRequest) => {
  console.log('new', newSearchParams)

  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  console.log('searchparams', searchParams.value)
  fetchData();
}
</script>

<style scoped>
.page-home .tag-bar {
  padding-bottom: 20px;
}
</style>
