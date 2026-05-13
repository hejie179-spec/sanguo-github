import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import './styles/global.scss'

// 前端应用入口文件，相当于整个前端的启动开关
// 第一步：创建Vue应用实例
// 第二步：挂载Pinia，用来管理全局状态，比如用户登录信息什么的
// 第三步：挂载Router，就是管理页面跳转的，比如从登录页跳到首页
// 第四步：挂载Element Plus，这是个UI组件库，用它来做界面，顺便把语言设置成中文
// 最后：把应用挂载到页面上的#app元素里
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
