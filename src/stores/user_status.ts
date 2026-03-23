import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginUserUsingGet } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.UserVO>({
    userAvatar: "https://qw-1346071538.cos.ap-nanjing.myqcloud.com///space/6/2025-11-16_7692c8d1-13c6-4bf7-964b-013496d4b7d4.b8.png",
  })

  const fetchLoginUser = async () => {
    // todo 这里后端开发完全后在进行 '获取'
    const res = await getLoginUserUsingGet(); // 这个 getCurrentUser方法后续再添加
    if (res.data.code === 0 && res.data.data) {
      setLoginUser(res.data?.data);
    } else {
      // 如果请求的是 user/login  user/register 就不报错
      if (!window.location.pathname.includes("/user/login") &&
      !window.location.pathname.includes("/user/register")){
        message.error("请先登录");
      }
    }

    // 模拟请求获取用户状态
    // setTimeout(() => {
    //   loginUser.value = {username: "鞠婧祎", id : 666};
    // }, 3000);
  }

  const setLoginUser = (newLoginUser: any) => {
    loginUser.value = newLoginUser
  }

  return {loginUser, setLoginUser, fetchLoginUser};
})
