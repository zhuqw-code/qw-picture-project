<template>
  <div id="userLoginPage">
    <h1 class="title">qw智能云图库 ---- 用户登录页面</h1>
    <div class="desc">企业级智能协同云图库</div>
    <a-form
      :model="formState"
      name="basic"
      autocomplete="off"
      @finish="submitHandle"
      layout="vertical"
    >
      <a-form-item
        name="userAccount"
        aria-placeholder="请输入账号~"
        :rules="[{ required: true, message: '账号是必填项~' }, { min: 4 , message: '账号长度必须不小于4位'}]"
      >
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号~"/>
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[{ required: true, message: '密码是必填项'}, { min:8, message: '密码不小于8位'}]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码~" />
      </a-form-item>

      <div class="tips">
        没有账号？
        <router-link to="/user/register">还等什么</router-link>
      </div>

      <a-form-item>
        <!-- html-type="submit" 设置这个才能让表单知道点击之后触发提交 -->
        <a-button type="primary" html-type="submit" style="width:100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue';
import { userLoginUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import {useLoginUserStore} from '@/stores/user_status.ts'
import { useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore();

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
});

const router = useRouter();
const submitHandle = async (values: any) => {
  // 像后端发送请求
  const res = await userLoginUsingPost(formState);
  if (res.data.code === 0 && res.data.data) {
    // 登录成功让pinia发送请求获取当前登录用户
    message.success(res.data.message);
    await loginUserStore.fetchLoginUser();
    // 跳转到主页
    // 不能跳转的原因是路由配置错误
    await router.push({
      path: "/",
      replace: true,
    })
  } else {
    message.error("登录失败：" + res.data.message);
  }
};
</script>

<style scoped>
#userLoginPage{
  max-width: 600px;
  margin: 0 auto;
}


.title{
  text-align: center;
  padding: 40px;
}

.desc{
  color: #6cbcd2;
  font-size: 24px;
  text-align: center;
  margin-bottom: 40px;
}

.tips{
  text-align: right;
  padding: 10px;
}

:where(.css-dev-only-do-not-override-1p3hq3p){
  min-height: 56px;
  margin-top: 32px;
}
</style>
