<template>
  <div id="add-space">
    <h2 style="margin-bottom: 16px; text-align: center">
      {{ spaceId ? '编辑空间页面' : '创建空间页面' }}
    </h2>
    <a-form :model="spaceForm" layout="vertical" @finish="submitHandle">
      <a-form-item label="空间昵称" name="spaceName" required>
        <a-input v-model:value="spaceForm.spaceName" placeholder="名称" allow-clear />
      </a-form-item>
      <!-- 你敢想，不设置name 数据不能正常修改 -->
      <a-form-item label="空间级别" name="level">
        <a-select
          v-model:value="spaceForm.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          allow-clear
          style="min-width: 160px"
          default-active-first-option
        >
        </a-select>
      </a-form-item>
      <a-form-item>
        <!-- 不为button设置 html-type="submit" 就不会识别到提交按钮，也不会触发 @finish事件 -->
        <a-button type="primary" style="width: 100%" html-type="submit"
          >{{ spaceId ? '提交编辑' : '新增空间' }}
        </a-button>
      </a-form-item>
    </a-form>
    <a-card title="空间介绍">
      <a-flex>
        <a-typography-paragraph>
          <a href="#" target="_blank">扩容私有空间请联系管理员</a>
        </a-typography-paragraph>
        <a-typography-paragraph v-for="spaceLevel in spaceLevels">
          {{spaceLevel.text}} : 大小 {{spaceLevel.maxSize}} -------- 条数{{spaceLevel.maxCount}}
        </a-typography-paragraph>
      </a-flex>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  updateSpaceUsingPut,
} from '@/api/spaceController.ts'
import { SPACE_LEVEL_OPTIONS } from '@/constant/sapce.ts'
import { listSpaceLevelUsingGet } from '@/api/pictureController.ts'

const spaceId = ref()
const space = ref<API.SpaceVO>()
// 真SB还必须写成对象类型
const spaceForm = reactive<API.SpaceAddRequest | API.SpaceUpdateRequest>({})
// 空间等级列表，展示给用户方便更新等级
const spaceLevels = ref<API.SpaceLevel[]>([]);
/**
 * 提交表单
 * 发送请求
 */
const router = useRouter()
const submitHandle = async () => {
  // 封装查询参数 id，上传和编辑其实都是编辑，在空间upload后已经在数据库中了
  const spaceId = space.value?.id
  let res = null
  console.log(spaceForm)
  // 1. 添加
  if (!spaceId) {
    res = await addSpaceUsingPost({
      ...spaceForm,
    })
  }
  // 2.编辑
  // 调用编辑方法将空间信息保存，因为空间加载成功后就保存在数据库中了
  else {
    res = await updateSpaceUsingPut({
      id: spaceId,
      ...spaceForm,
    })
  }
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功！')
    await router.push(`/space/${spaceId}`)
  } else {
    message.error('操作失败！' + res.data.message)
  }
}

/**
 * 1.添加 / 修改页面
 * 修改页面相较添加，会将一些字段显示出来
 * 2.如何区分两个页面
 * 修改页面路径后面有id
 */
const route = useRoute()
const getOldSpace = async () => {
  // 是否要将旧页面加载出来，判断是不是更新页面，判断是否有id
  const id = route.query?.id
  if (!id) {
    return
  }
  const res = await getSpaceVoByIdUsingGet({ id })
  {
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
      Object.assign(spaceForm, data)
    }
  }
}

/**
 * 获取空间等级信息
 */
const fetchSpaceLevels = async () => {
  const res = await listSpaceLevelUsingGet();
  if (res.data.code === 0 && res.data.data)  {
    spaceLevels.value = res.data.data;
  }
}

// 如果是更新就要渲染数据
onMounted(() => {
  getOldSpace()
  spaceId.value = route.query?.id
  fetchSpaceLevels();
})
</script>

<style scoped>
#add-space {
  max-width: 720px;
  margin: 0 auto;
}
</style>
