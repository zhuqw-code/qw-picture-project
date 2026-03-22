<template>
  <div id="add-picture">
    <h2 style="margin-bottom: 16px; text-align: center">
      {{ route.query?.id ? '修改图片页面' : '创建图片页面' }}
    </h2>
    <a-tabs v-model:activeKey="active" centered size="large" type="line" :tab-bar-gutter="160">
      <a-tab-pane key="1" tab="本地上传">
        <PictureUpload :picture="picture" :space-id="spaceId" :on-success="uploadHandle" />
      </a-tab-pane>
      <a-tab-pane key="2" tab="url上传" force-render>
        <UrlPictureUpload :picture="picture" :space-id="spaceId" :on-success="uploadHandle" />
      </a-tab-pane>
      <a-tab-pane key="3" tab="待开发"> xxx</a-tab-pane>
    </a-tabs>
    <div v-if="picture" class="edit-bar">
      <a-space size="middle">
        <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
        <a-button type="primary" ghost :icon="h(FullscreenOutlined)" @click="doImagePainting">
          AI 扩图
        </a-button>
      </a-space>
      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture.url"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onCropSuccess"
      />
      <ImageOutPainting
        ref="imageOutPaintingRef"
        :picture="picture"
        :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess"
      />
    </div>
    <a-form v-if="picture" :model="pictureForm" layout="vertical" @finish="submitHandle">
      <a-form-item label="名称" name="name">
        <a-input v-model:value="pictureForm.name" placeholder="名称" :allow-clear="true" />
      </a-form-item>
      <!-- 你敢想，不设置name 数据不能正常修改 -->
      <a-form-item label="介绍" name="introduction">
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="请输入图片介绍"
          allow-clear
          :autosize="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>
      <a-form-item label="分类" name="category">
        <a-auto-complete
          v-model:value="pictureForm.category"
          :options="categoryOptions"
          placeholder="输入分类"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          placeholder="输入图片标签"
          :options="tagsOptions"
        />
      </a-form-item>
      <a-form-item>
        <!-- 不为button设置 html-type="submit" 就不会识别到提交按钮，也不会触发 @finish事件 -->
        <a-button type="primary" style="width: 100%" html-type="submit">提交修改</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  editPictureUsingPut,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'

const picture = ref<API.PictureVO>()
// 真SB还必须写成对象类型
const pictureForm = reactive<API.PictureEditRequest>({})

const active = ref<string>('1')

// todo 获取到 spaceId 传递给图片上传组件
const route = useRoute()
const spaceId = computed(() => {
  return route.query?.spaceId
})

/**
 * 回调函数，图片正常上传后调用该方法
 * @param newPicture 获取到子组件传递的图片信息
 */
const uploadHandle = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

/**
 * 提交表单
 * 发送请求
 */
const router = useRouter()
const submitHandle = async (formData: any) => {
  // 封装查询参数 id，上传和编辑其实都是编辑，在图片upload后已经在数据库中了
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  // 调用编辑方法将图片信息保存，因为图片加载成功后就保存在数据库中了
  const res = await editPictureUsingPut({
    id: pictureId,
    ...formData,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功！')
    await router.push(`/picture/${pictureId}`)
  } else {
    message.error('操作失败！' + res.data.message)
  }
}

// 下拉框 和 标签
const categoryOptions = ref<string[]>([])
const tagsOptions = ref<string[]>([])
onMounted(async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    tagsOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    console.log(tagsOptions.value)
  } else {
    message.error('获取分类标签失败！')
  }
})

/**
 * 1.添加 / 修改页面
 * 修改页面相较添加，会将一些字段显示出来
 * 2.如何区分两个页面
 * 修改页面路径后面有id
 */
// console.log(route.query.id)
const getOldPicture = async () => {
  // 是否要将旧页面加载出来，判断是不是更新页面，判断是否有id
  const id = route.query?.id // 修改图片之前需要获取旧图片信息
  if (!id) {
    return
  }
  const res = await getPictureVoByIdUsingGet({ id })
  {
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      Object.assign(pictureForm, data)
    }
  }
}
// 如果是更新就要渲染数据
onMounted(() => {
  getOldPicture()
})

/**
 * 新增图片编辑
 */
// 图片编辑弹窗引用
const imageCropperRef = ref()

// 编辑图片
const doEditPicture = () => {
  if (imageCropperRef.value) {
    imageCropperRef.value.openModal()
  }
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ai扩图功能
// AI 扩图弹窗引用
const imageOutPaintingRef = ref()

// AI 扩图
const doImagePainting = () => {
  if (imageOutPaintingRef.value) {
    imageOutPaintingRef.value.openModal()
  }
}

// 编辑成功事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}
</script>

<style scoped>
#add-picture {
  max-width: 720px;
  margin: 0 auto;
}

#add-picture .edit-bar {
  text-align: center;
  margin: 16px 0;
}
</style>
