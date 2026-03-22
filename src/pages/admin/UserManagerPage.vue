<template>
  <div class="user-manager">
    <!-- 搜索栏 -->
    <a-form :model="searchParams" layout="inline" @finish="doSearch">
      <a-form-item label="id">
        <a-input v-model:value="searchParams.id" placeholder="id查询" :allow-clear="true" />
      </a-form-item>
      <a-form-item label="账号">
        <a-input
          v-model:value="searchParams.userAccount"
          placeholder="账号查询"
          :allow-clear="true"
        />
      </a-form-item>
      <a-form-item label="昵称">
        <a-input v-model:value="searchParams.userName" placeholder="昵称查询" :allow-clear="true" />
      </a-form-item>
      <a-form-item>
        <!-- 不为button设置 html-type="submit" 就不会识别到提交按钮，也不会触发 @finish事件 -->
        <a-button type="primary" html-type="submit">Submit</a-button>
      </a-form-item>
      <!-- 添加上传图片页面 -->
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
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="64" :height="64" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <span v-if="record.userRole === 'admin'">
            <a-tag color="pink">管理员</a-tag>
          </span>
          <span v-else-if="record.userRole === 'user'">
            <a-tag color="blue">用户</a-tag>
          </span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>
            <a-tag color="green">
              {{ dayjs(record.createTime).format('YYYY-MM-DD hh-mm-ss') }}
            </a-tag>
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <span>
            <a-tag color="red" @click="doDelete(record.id)">删除</a-tag>
          </span>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUserUsingDelete, listUserVoByPageUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import AddPictureBatchPage from '@/pages/admin/AddPictureBatchPage.vue'

const columns = [
  {
    name: 'id',
    dataIndex: 'id',
    title: '编号',
    key: 'id',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    ellipsis: true,
  },
  {
    title: '角色',
    dataIndex: 'userRole',
    key: 'userRole',
  },
  {
    title: '用户介绍',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: 'Action',
    key: 'action',
  },
]

const tableData = ref<API.UserVO[]>()

// 编写请求函数，向后端获取用户信息
const total = ref(0)
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

/**
 * 获取用户信息
 */
const fetchData = async () => {
  const res = await listUserVoByPageUsingPost({
    ...searchParams,
  })
  if (res.data.code == 0 && res.data.data) {
    message.success('加载成功~')
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
  fetchData()
}

// 删除数据
const doDelete = async (id: number) => {
  const res = await deleteUserUsingDelete({ id })
  if (res.data.code === 0) {
    message.success('删除成功~')
    // 重新加载数据
    fetchData()
  } else {
    message.error('删除失败：' + res.data.message)
  }
}
</script>

<style scoped></style>
