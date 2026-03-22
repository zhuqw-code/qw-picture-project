<template>
  <a-modal
    class="image-cropper"
    v-model:visible="visible"
    title="编辑图片"
    :footer="false"
    @cancel="closeModal"
  >
    <vue-cropper
      ref="cropperRef"
      :img="cropImgSrc"
      :autoCrop="true"
      :fixedBox="false"
      :centerBox="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />
    <div style="margin-bottom: 16px" />
    <!-- 图片操作 -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft">向左旋转</a-button>
        <a-button @click="rotateRight">向右旋转</a-button>
        <a-button @click="changeScale(1)">放大</a-button>
        <a-button @click="changeScale(-1)">缩小</a-button>
        <a-button type="primary" :loading="loading" @click="handleConfirm">确认</a-button>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'
import request from '@/request'
import { message } from 'ant-design-vue'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

/** 与 request.baseURL 同源的图走接口拉 blob，再交给 cropper，避免 OSS 外链无 CORS 时加载失败 */
function getApiOrigin(): string {
  const base = request.defaults.baseURL || ''
  try {
    return new URL(base, window.location.href).origin
  } catch {
    return ''
  }
}

async function resolveImageForCropper(raw: string): Promise<string> {
  if (!raw.trim()) return ''
  const apiOrigin = getApiOrigin()
  if (!apiOrigin) return raw

  let absolute = raw.trim()
  if (absolute.startsWith('/')) {
    absolute = `${apiOrigin}${absolute}`
  }

  let parsed: URL
  try {
    parsed = new URL(absolute)
  } catch {
    return raw
  }

  if (parsed.origin !== apiOrigin) {
    return raw
  }

  const path = `${parsed.pathname}${parsed.search}`
  const res = await request.get(path, { responseType: 'blob' })
  return URL.createObjectURL(res.data as Blob)
}

const cropImgSrc = ref('')
const blobObjectUrl = ref<string | null>(null)

function revokeBlobIfAny() {
  if (blobObjectUrl.value) {
    URL.revokeObjectURL(blobObjectUrl.value)
    blobObjectUrl.value = null
  }
}

// 编辑器组件的引用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
}

// 是否可见
const visible = ref(false)

// 打开弹窗（先解析同源图为 blob，再打开，避免弹层内容器为 0 时误布局）
const openModal = async () => {
  revokeBlobIfAny()
  const raw = props.imageUrl ?? ''
  try {
    const resolved = await resolveImageForCropper(raw)
    if (resolved.startsWith('blob:')) {
      blobObjectUrl.value = resolved
    }
    cropImgSrc.value = resolved
  } catch {
    cropImgSrc.value = raw
    message.warning('无法经后端拉取该图片，将直接使用原始地址（跨域时可能仍失败）')
  }
  visible.value = true
  await nextTick()
  await nextTick()
  cropperRef.value?.refresh?.()
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
  revokeBlobIfAny()
  cropImgSrc.value = ''
}

onBeforeUnmount(revokeBlobIfAny)

// 暴露函数给父组件
defineExpose({
  openModal,
})

const loading = ref<boolean>(false)

// 确认裁剪
const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    const fileName = (props.picture?.name || 'image') + '.png'
    const file = new File([blob], fileName, { type: blob.type })
    // 上传图片
    handleUpload({ file })
  })
}

/**
 * 上传
 * @param file
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px;
}
</style>
