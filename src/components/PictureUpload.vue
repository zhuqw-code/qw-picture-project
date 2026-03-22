<template>
  <div id="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
      class="ant-upload"
    >
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">上传图片</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { ref } from 'vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

/**
 * 子组件将图片上传到服务器，关键是将响应信息传递给父组件
 */

interface Props {
  picture?: API.PictureVO;
  // todo 因为要针对不用空间上传不同图片，足以query 参数是否有 spaceId字段
  spaceId?: number;
  onSuccess?: (responsePicture: API.PictureVO) => {};
}

const props = defineProps<Props>()

const loading = ref<Boolean>(false)
/**
 * 将上传图片信息传递给服务器
 * @param file 图片文件
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.uploadPictureUsingPOSTParams = {
      id: props.picture?.id,
      spaceId: props?.spaceId
    }
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('文件上传成功!')
      // 信息传递给父组件的回调函数，赋值给父组件参数
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败！' + res.data.message)
    }
  } catch (error) {
    message.error('error！' + error)
  }

  loading.value = false
}

const beforeUpload = (file: UploadProps['fileList'][number]) => {
  const isJpgOrPng =
    file.type === 'image/jpeg' ||
    file.type === 'image/png' ||
    file.type === 'image/webp' ||
    file.type === 'image/jpg'
  if (!isJpgOrPng) {
    message.error('图片格式只能为jpeg,png,jpg,webp!')
  }
  const isLt2M = file.size / 1024 / 1024 < 6.6
  if (!isLt2M) {
    message.error('图片只能小于 6MB!')
  }
  return isJpgOrPng && isLt2M
}
</script>
<style scoped>
#picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}

#picture-upload img {
  max-height: 480px;
  max-width: 100%;
}
</style>
