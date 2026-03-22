<template>
  <div id="userRegisterPage">
    <h1 class="title">qw智能云图库 ---- 用户注册页面</h1>
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

      <a-form-item
        name="checkPassword"
        :rules="[{ required: true, message: '确认密码是必填项'}, { min:8, message: '确认密码不小于8位'}]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码~" />
      </a-form-item>

      <div class="tips">
        已有账号？
        <router-link to="/user/login">直接登录就完了</router-link>
      </div>

      <a-form-item>
        <!-- html-type="submit" 设置这个才能让表单知道点击之后触发提交 -->
        <a-button type="primary" html-type="submit" style="width:100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue';
import { userLoginUsingPost, userRegisterUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import {useLoginUserStore} from '@/stores/user_status.ts'
import router from '@/router'

const registerUserStore = useLoginUserStore();

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
});
const submitHandle = async (values: any) => {
  // 判断两次登录密码是否一致
  if (values.userPassword !== values.checkPassword) {
    message.error("两次密码不一致哦~");
    return;
  }
  // 像后端发送请求
  const res = await userRegisterUsingPost(formState);
  if (res.data.code === 0 && res.data.data) {
    // 注册成功让pinia发送请求获取当前注册用户
    message.success(res.data.message);
    // 跳转到登录
    await router.push({
      path: "/user/login",
      replace: true,
    });
  } else {
    message.error("注册失败：" + res.data.message);
  }
};
</script>

<style scoped>
#userRegisterPage{
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
