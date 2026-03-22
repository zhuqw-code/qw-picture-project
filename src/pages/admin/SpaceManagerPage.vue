<template>
  <div class="Space-manager">
    <!-- 搜索栏 -->
    <a-form :model="searchParams" layout="inline" @finish="doSearch">
      <a-form-item label="空间id">
        <a-input v-model:value="searchParams.id" placeholder="id查询" allow-clear />
      </a-form-item>
      <a-form-item label="用户id">
        <a-input
          v-model:value="searchParams.userId"
          placeholder="根据用户id查询用户空间"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="空间名称">
        <a-input v-model:value="searchParams.spaceName" placeholder="id查询" allow-clear />
      </a-form-item>
      <a-form-item name="spaceLevel" label="空间等级">
        <a-select
          v-model:value="searchParams.spaceLevel"
          style="min-width: 160px"
          default-active-first-option
          placeholder="请输入查询的空间级别"
          :options="SPACE_LEVEL_OPTIONS"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <!-- 不为button设置 html-type="submit" 就不会识别到提交按钮，也不会触发 @finish事件 -->
        <a-button type="primary" html-type="submit" style="min-width: 200px">Submit</a-button>
      </a-form-item>
      <!--其实能够看到管理页就一定是管理员，不用再判断了-->
      <!--<AddSpaceBatchPage v-if="useLoginUserStore().loginUser.userRole === 'admin'" />-->
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
        <template v-if="column.dataIndex === 'spaceLevel'">
          {{ SPACE_LEVEL_MAP[record.spaceLevel] }}
        </template>
        <template v-else-if="column.dataIndex === 'useInfo'">
          <a-space direction="vertical">
            <span>大小：{{ record.totalSize }} / {{ record.maxSize }}</span>
            <span>数量：{{ record.totalCount }} / {{ record.maxCount }}</span>
          </a-space>
        </template>
        <template v-else-if="column.dataIndex === 'userInfo'">
          <a-space direction="vertical">
            <a-tag color="blue"> 昵称 ：{{ record.user.userName }}</a-tag>
            <a-tag color="blueViolet"> 角色 ：{{ record.user.userRole }}</a-tag>
            <a-tag color="pink"> 账号：{{ record.user.userAccount }}</a-tag>
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
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button style="background-color: orangered" @click="doDelete(record.id)"> 删除 </a-button>
            <router-link :to="`/add_space?id=${record.id}`">
              <a-button style="background-color: aquamarine"> 操作 </a-button>
            </router-link>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteSpaceUsingDelete } from '@/api/SpaceController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS } from '../../constant/sapce.ts'

const columns = [
  {
    name: 'id',
    dataIndex: 'id',
    title: '编号',
    width: 80,
  },
  {
    title: '空间名称',
    dataIndex: 'spaceName',
  },
  {
    title: '空间级别',
    dataIndex: 'spaceLevel',
  },
  {
    title: '使用信息',
    dataIndex: 'useInfo',
  },
  {
    title: '用户信息',
    dataIndex: 'userInfo',
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
    dataIndex: 'action',
  },
]

const tableData = ref<API.Space[]>()

// 编写请求函数，向后端获取空间信息
const total = ref(0)
const searchParams = reactive<API.SpaceQueryRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

/**
 * 获取空间信息
 */
const fetchData = async () => {
  const res = await listSpaceVoByPageUsingPost({
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
  const res = await deleteSpaceUsingDelete({ id })
  if (res.data.code === 0) {
    message.success('删除成功~')
    // 重新加载数据
    fetchData()
  } else {
    message.error('删除失败：' + res.data.message)
  }
}
// 编辑数据
const router = useRouter()
const doUpdate = (id: number) => {
  router.push({
    path: `/Space/${id}`,
  })
}
</script>

<style scoped></style>
