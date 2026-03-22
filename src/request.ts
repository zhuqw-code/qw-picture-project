import axios from 'axios'
import { message } from 'ant-design-vue'

/**
 * 全局请求配置
 */
const instance = axios.create({
  baseURL: 'http://localhost:8123',
  timeout: 60000, // 请求不能超过 60s
  withCredentials: true, // 请求是否携带cookie
})

/**
 * 拦截器配置
 */
// 添加请求拦截器
axios.interceptors.request.use(
  (config) => {
    // 在请求之前如果访问的路径是非普通页面，就截断请求，并提示用户先登录
    console.log(666);
    return config;
  },
  function (error) {
    // 对请求错误做些什么
    return Promise.reject(error)
  },
)

// 添加响应拦截器
axios.interceptors.response.use(
  function (response) {
    const { data } = response // 为什么要加{}
    if (data.data.code === 0) {
      return;
    }
    if (
      !response.request.responseURL.includes('user/get/login') ||
      !window.location.pathname.includes('/user/login')
    ) {
      // 40100 表示用户未登录
      message.warning('登录后方可执行该操作')
      // 重定向技术：如果用户想要执行某些权限操作，我们就让他登录，登录成功后自动重定向到预期页面
      window.location.href = `/user/login?redirect=${window.location.href}`
    } else {
      message.error(data.message);
    }
    return response
  },
  function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    return Promise.reject(error)
  },
)

export default instance
