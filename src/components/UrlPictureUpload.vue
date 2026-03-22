<template>
  <div id="picture-upload">
    <div style="text-align: center">
      <a-image
        class="image"
        v-if="picUrl"
        :src="picUrl ?? props?.picture?.url"
        style="max-height: 500px; padding-bottom: 20px"
      />
    </div>
    <a-input-group compact size="large">
      <a-input v-model:value="picUrl" style="width: calc(100% - 81px)" />
      <a-button type="primary" :loading="loading" @click="urlUploadHandler">Submit</a-button>
    </a-input-group>
  </div>
</template>
<script lang="ts" setup>
import { message } from 'ant-design-vue'
import { ref } from 'vue'
import { uploadPictureByUrlUsingPost } from '@/api/pictureController.ts'

/**
 * 子组件将图片上传到服务器，关键是将响应信息传递给父组件
 */

interface Props {
  picture?: API.PictureVO
  // todo 因为要针对不用空间上传不同图片，足以query 参数是否有 spaceId字段
  spaceId?: number;
  onSuccess?: (responsePicture: API.PictureVO) => {}
}

const props = defineProps<Props>()

const loading = ref<Boolean>(false)

const picUrl = ref<String>('')
/**
 * 将上传图片信息传递给服务器
 */

const urlUploadHandler = async () => {
  // 封装请求参数（?id, url）
  const params: API.uploadPictureByUrlUsingPOSTParams = {
    fileUrl: picUrl.value as string,
    // todo 为啥这里无法传递给后端
    spaceId: props.spaceId,
  } // 因为我没有取 value
  if (props.picture) {
    params.id = props.picture.id;
  }
  try {
    const res = await uploadPictureByUrlUsingPost(params, {})
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
// 只根据url不能在前端检验图片
</script>
<style scoped>
#picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}
</style>
