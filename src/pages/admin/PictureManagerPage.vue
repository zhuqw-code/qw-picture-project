<template>
  <div class="picture-manager">
    <!-- 搜索栏 -->
    <a-form :model="searchParams" layout="inline" @finish="doSearch">
      <a-form-item label="id">
        <a-input v-model:value="searchParams.id" placeholder="id查询" :allow-clear="true" />
      </a-form-item>
      <a-form-item label="图片内容">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="请输入图片突出信息"
          :allow-clear="true"
        />
      </a-form-item>
      <a-form-item label="标签">
        <a-input v-model:value="searchParams.tags" placeholder="标签查询" :allow-clear="true" />
      </a-form-item>
      <a-form-item label="图片状态">
        <a-select
          v-model:value="searchParams.reviewStatus"
          style="width: 200px"
          :options="PIC_REVIEW_STATUS_OPTIONS"
        ></a-select>
      </a-form-item>
      <a-form-item>
        <!-- 不为button设置 html-type="submit" 就不会识别到提交按钮，也不会触发 @finish事件 -->
        <a-button type="primary" html-type="submit" style="min-width: 200px">Submit</a-button>
      </a-form-item>
      <!--其实能够看到管理页就一定是管理员，不用再判断了-->
      <!--<AddPictureBatchPage v-if="useLoginUserStore().loginUser.userRole === 'admin'" />-->
      <AddPictureBatchPage />
    </a-form>
    <div style="height: 16px"></div>
    <!-- 表格组件   -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      size="large"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image :src="record.url" :width="64" :height="64" />
        </template>
        <template v-else-if="column.dataIndex === 'picInfo'">
          <a-space wrap>
            <span>格式：{{ record.picFormat }}</span>
            <span>大小：{{ record.picSize }}</span>
            <span>高度：{{ record.picHeight }}</span>
            <span>宽度：{{ record.picWidth }}</span>
            <span>纵横比：{{ record.picScale }}</span>
          </a-space>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>
            <a-tag color="green">
              {{ dayjs(record.createTime).format('YYYY-MM-DD hh-mm-ss') }}
            </a-tag>
          </span>
        </template>
        <template v-else-if="column.dataIndex === 'editTime'">
          <span>
            <a-tag color="blue">
              {{ dayjs(record.editTime).format('YYYY-MM-DD hh-mm-ss') }}
            </a-tag>
          </span>
        </template>
        <template v-else-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag color="pink" v-for="(item, idx) in JSON.parse(record.tags)" :key="idx">
              {{ item }}
            </a-tag>
          </a-space>
        </template>
        <template v-else-if="column.dataIndex === 'reviewMessage'">
          <div>审核状态：{{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}</div>
          <div>审核信息：{{ record.reviewMessage }}</div>
          <div>审核 人：{{ record.reviewerId }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
              type="primary"
              @click="reviewHandler(record, PIC_REVIEW_STATUS_ENUM.PASS)"
              >通过
            </a-button>
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
              type="primary"
              danger
              @click="reviewHandler(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
              >拒绝
            </a-button>
            <a-button style="background-color: #ff6082" @click="doDelete(record.id)">删除</a-button>
            <router-link :to="`/add_picture?id=${record.id}`">
              <a-button
                style="background-color: aquamarine"
                color="blueViolet"
                @click="doUpdate(record.id)"
                >编辑
              </a-button>
            </router-link>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deletePictureUsingDelete,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
  listPictureVoByPageUsingPost,
} from '@/api/PictureController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '../../constant/picture.ts'
import AddPictureBatchPage from '@/pages/admin/AddPictureBatchPage.vue'
import { useLoginUserStore } from '@/stores/user_status.ts'
import { useRouter } from 'vue-router'

const columns = [
  {
    name: 'id',
    dataIndex: 'id',
    title: '编号',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
    ellipsis: true,
  },
  {
    title: '状态参数',
    dataIndex: 'reviewMessage',
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
  },
  {
    title: '操作',
    key: 'action',
  },
]

const tableData = ref<API.Picture[]>()

// 编写请求函数，向后端获取图片信息
const total = ref(0)
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  sortOrder: 'descend',
})

/**
 * 获取图片信息
 */
const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...searchParams,
  })
  if (res.data.code == 0 && res.data.data) {
    // message.success('加载成功~')
    tableData.value = res.data.data.records ?? []
    total.value = parseInt(res.data.data.total ?? '0')
  } else {
    message.error('加载失败：' + res.data.message)
  }
}

// 页面加载时获取
onMounted(() => {
  fetchData()
})

// 分页查询
// 因为使用计算属性，改变时会被 @chang 捕获到执行 doTableChange 发送请求获取最新数据
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: any) => `共${total}条`,
  }
})

// 查询条件变化重新查询
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  // 重新加载数据
  console.log('通过修改分页触发doTableChange')
  fetchData()
}

// 查询数据
const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: number) => {
  const res = await deletePictureUsingDelete({ id })
  if (res.data.code === 0) {
    message.success('删除成功~')
    // 重新加载数据
    fetchData()
  } else {
    message.error('删除失败：' + res.data.message)
  }
}
// 编辑数据
const router = useRouter();
const doUpdate = (id: number) => {
  router.push({
    path: `/picture/${id}`,
  })
}

// 给状态按钮绑定时间处理函数
const reviewHandler = async (picture: API.Picture, reviewStatus: number) => {
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员操作通过' : '管理员拒绝通过'
  const res = await doPictureReviewUsingPost({
    id: picture.id,
    reviewStatus: reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('审核通过！')
    await fetchData() // 需要给表单添加 status 的查询字段
  } else {
    message.error('审核失败！')
  }
}
</script>

<style scoped></style>
