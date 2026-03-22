// 用户权限校验
// 思路在用户跳转页面之前进行判断是否登录，是否有权限
// ？ 为什么后端进行校验了还要在前端校验。防止用户恶意攻击服务器，在前端进行一些必要拦截
// ？ 该 ts 文件会在每次刷新浏览器重新加载（通过控制台的请求中可以看到）

import router from '@/router'
import { useLoginUserStore } from '@/stores/user_status.ts'
import { message } from 'ant-design-vue'

// 首次刷新浏览器
let firstFetchLoginUser = true;
console.log("刷新浏览器就执行一次，获取到当前用户~");

// 用户可能会有多次跳转操作
router.beforeEach(async (to, from, next) => {
  // 获取用户信息，之后进行权限校验
  const loginUserStore = useLoginUserStore();
  let currentUser = loginUserStore.loginUser;
  if (!to.path.startsWith("/admin")){
    next();
    return;
  }
  if (firstFetchLoginUser) {
    // 获取用户信息，并更新全局用户状态
    await loginUserStore.fetchLoginUser();
    currentUser = loginUserStore.loginUser;
  }
  // 如果用户没有登录，或者没有权限想要访问管理员页面
  if (to.path.startsWith("/admin")){
    console.log(currentUser);
    if (!currentUser || !currentUser.id){
      message.error("请先登录cd~");
      // 传递数据就默认有错误
      // todo 有问题
      window.location.href = `/user/login?redirect=${to.fullPath}`;
      return;
    }
    else if (currentUser.userRole !== "admin"){
      message.error("无权限~");
      // 传递数据就默认有错误
      next("/");
      return;
    }
  }
  // 不传数据就默认放行
  next();
})

