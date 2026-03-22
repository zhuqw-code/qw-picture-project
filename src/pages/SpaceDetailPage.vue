<template>
  <div id="space-detail-page">
    <!--  空间信息展示  -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}（私有空间）</h2>
      <a-space>
        <a-button type="primary" :href="`/add_picture?spaceId=${spaceId}`"> +创建图片</a-button>
        <a-tooltip :title="`占用空间${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`">
          <a-progress
            type="circle"
            :size="48"
            :percent="Number((((space.totalSize as number) * 100) / (space.maxSize as number)).toFixed(1))"
          />
        </a-tooltip>
      </a-space>
    </a-flex>
    <PictureSearchForm :on-search="onSearch" />
    <!-- 添加根据主色调查询的表单项  -->
    <!-- 按颜色搜索 -->
    <a-form-item label="按颜色搜索" style="margin-top: 16px">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>
    <!--  图片信息展示  -->
    <PictureList :picture-list="pictureList" :loading="loading" :show-op="true" />
    <!--  分页器  -->
    <a-pagination
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      show-less-items
      @change="doMenuChange"
      style="text-align: right; padding-bottom: 16px"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { formatSize } from '@/utils/tools.ts'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
// 颜色选择器
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'

interface Props {
  spaceId: number | string
}

const props = defineProps<Props>()

const space = ref<API.SpaceVO>({})

const pictureList = ref<API.PictureVO[] | undefined>([])
const loading = ref<boolean>(false)

// 分页
const total = ref<number>()
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// ----------------获取我的私有空间信息
const fetchSpaceDetail = async () => {
  const res = await getSpaceVoByIdUsingGet({
    id: props.spaceId as number,
  })
  if (res.data.code === 0 && res.data.data) {
    space.value = res.data.data
    // message.success('获取成功!')
  } else {
    message.error('获取空间失败！')
  }
}

// ----------------获取我的私有空间图片
const fetchMyPictures = async () => {
  const res = await listPictureVoByPageUsingPost({
    ...searchParams.value,
    spaceId: props.spaceId as number,
  })
  if (res.data.code === 0 && res.data.data) {
    pictureList.value = res.data.data.records
    total.value = Number(res.data.data?.total)
    // message.success('获取成功!')
  } else {
    message.error('获取失败！' + res.data.message)
  }
}

// 搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  console.log('new', newSearchParams)

  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  console.log('searchparams', searchParams.value)
  fetchMyPictures()
}

onMounted(() => {
  fetchSpaceDetail()
  fetchMyPictures()
})

/**
 * 分页器变化重新获取数据
 */
const doMenuChange = (current: number, pageSize: number) => {
  searchParams.value.current = current
  searchParams.value.pageSize = pageSize
  fetchMyPictures()
}

/**
 *  按照颜色搜索
 */
const onColorChange = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.spaceId as number,
  })
  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []
    pictureList.value = data
    total.value = data.length
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
</script>
