<template>
  <div id="add_picture_batch">
    <a-button size="large" class="button" type="primary" ghost @click="open = true">批量抓图</a-button>
    <a-drawer
      title="管理员可批量抓图"
      placement="right"
      :closable="true"
      :open="open"
      @close="open = false"
      :width="800"
      style="background-color: pink"
    >
      <a-form style="max-width: 400px" size="large" @submit="doBatchPictureHandle">
        <a-form-item required>
          <a-input v-model:value="batchParam.searchText" type="text" placeholder="请输入要抓取的内容"></a-input>
        </a-form-item>
        <a-form-item placeholder="请选择记录条数">
          <a-input-number style="min-width: 400px" v-model:value="batchParam.count" :max="30" default-value="5" />
        </a-form-item>
        <a-form-item placeholder="请给图片提供名称">
          <a-input v-model:value="batchParam.namePrefix" type="text" placeholder="请为抓取的图片命名"></a-input>
        </a-form-item>
        <a-form-item>
          <a-button style="width: 100%" type="primary" html-type="submit" :loading="loading">Submit</a-button>
        </a-form-item>
      </a-form>
    </a-drawer>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { type DrawerProps, message } from 'ant-design-vue'
import { uploadPictureByBatchUsingPost } from '@/api/pictureController.ts'
import { useRouter } from 'vue-router'

// 数据
const open = ref<boolean>(false)
// 请求参数
const batchParam = ref<API.PictureUploadByBatchRequest>({})
const loading = ref(false);
// 函数
// 抓取图片信息
const router = useRouter();
const doBatchPictureHandle = async () => {
  loading.value = true;
  const res = await uploadPictureByBatchUsingPost(batchParam.value);
  try{
    if (res.data.code === 0 && res.data.data) {
      message.success(`成功上传：${res.data.data}条记录！`);
      await router.push("/");
    }
    else {
      message.error("上传失败！" + res.data.message);
    }
  } catch(error : any){
    message.error("System Error: " + error);
  }
  loading.value = false;
}

</script>

<style scoped>
#add_picture_batch .button {
  position: absolute;
  right: 24px;
  top: 85px;
}
</style>
