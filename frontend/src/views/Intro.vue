<template>
  <div class="intro-page" :class="{ leaving }" @click="goHome">
    <div class="bg-layer" aria-hidden="true"></div>
    <div class="grain-layer" aria-hidden="true"></div>
    <div class="cloud cloud-1" aria-hidden="true"></div>
    <div class="cloud cloud-2" aria-hidden="true"></div>
    <div class="cloud cloud-3" aria-hidden="true"></div>

    <div class="content">
      <div class="title-wrap">
        <div class="seal" aria-hidden="true">蜀</div>
        <h1 class="title">三国文化主题网站</h1>
        <div class="subtitle">烽火连天三分鼎 · 风云人物一卷书</div>
      </div>

      <div class="hint">
        <span class="hint-dot" aria-hidden="true"></span>
        <span class="hint-text">点击任意处进入首页</span>
        <span class="hint-dot" aria-hidden="true"></span>
      </div>

      <div class="actions">
        <el-button type="primary" size="large" class="btn" @click.stop="goLogin">登录</el-button>
        <el-button size="large" class="btn" @click.stop="goRegister">注册</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

// 开场页（路由：/intro）
// - 未登录访问 / 会先被路由守卫重定向到这里
// - 点击屏幕/按钮进入登录页；也可直接去注册页
// - leaving 用于简单淡出动画，避免切页突兀
const router = useRouter()
const leaving = ref(false)

const go = (path) => {
  if (leaving.value) return
  leaving.value = true
  window.setTimeout(() => {
    router.push(path)
  }, 260)
}

const goHome = () => go({ path: '/', query: { fromIntro: '1' } })
const goLogin = () => go('/login')
const goRegister = () => go('/register')

const handleKeydown = (e) => {
  const key = e?.key
  if (key === 'Enter' || key === ' ') goHome()
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.intro-page {
  height: 100vh;
  width: 100%;
  overflow: hidden;
  position: relative;
  display: grid;
  place-items: center;
  cursor: pointer;
  user-select: none;
  background: #0f0b07;
}

.intro-page.leaving {
  opacity: 0.001;
  transform: scale(0.99);
  filter: blur(1px);
  transition: opacity 240ms ease, transform 240ms ease, filter 240ms ease;
}

.content {
  position: relative;
  z-index: 2;
  width: min(880px, calc(100% - 48px));
  border-radius: 18px;
  padding: 56px 28px 40px;
  text-align: center;
  cursor: default;
}

.title-wrap {
  display: grid;
  place-items: center;
  gap: 12px;
}

.seal {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 28px;
  letter-spacing: 2px;
  background: linear-gradient(145deg, #a11b1b, #6b0f0f);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35);
  transform: rotate(-6deg);
  animation: floatSeal 3.6s ease-in-out infinite;
}

.title {
  margin: 0;
  font-size: clamp(30px, 4vw, 48px);
  line-height: 1.15;
  letter-spacing: 4px;
  color: #f7f1e8;
  text-shadow: 0 14px 30px rgba(0, 0, 0, 0.35);
}

.subtitle {
  color: rgba(247, 241, 232, 0.75);
  font-size: 14px;
  letter-spacing: 2px;
}

.hint {
  margin-top: 26px;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.24);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(247, 241, 232, 0.85);
  cursor: pointer;
  appearance: none;
  font: inherit;
}

.hint-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e6a23c;
  box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.6);
  animation: pulse 1.6s ease-in-out infinite;
}

.hint-text {
  font-size: 13px;
  letter-spacing: 2px;
}

.actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}

.btn {
  min-width: 140px;
}

.bg-layer {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(900px 500px at 50% 25%, rgba(230, 162, 60, 0.18), rgba(0, 0, 0, 0) 65%),
    radial-gradient(900px 500px at 50% 75%, rgba(64, 158, 255, 0.12), rgba(0, 0, 0, 0) 60%),
    linear-gradient(180deg, #1a1410 0%, #0f0b07 38%, #0b0907 100%);
}

.grain-layer {
  position: absolute;
  inset: -40px;
  opacity: 0.16;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(255, 255, 255, 0.16) 1px, rgba(0, 0, 0, 0) 0);
  background-size: 10px 10px;
  filter: blur(0.2px);
  animation: drift 12s linear infinite;
}

.cloud {
  position: absolute;
  z-index: 1;
  width: 520px;
  height: 220px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 40%, rgba(247, 241, 232, 0.14), rgba(0, 0, 0, 0) 70%);
  filter: blur(2px);
  opacity: 0.9;
}

.cloud-1 {
  left: -120px;
  top: 12%;
  animation: cloudMove1 18s ease-in-out infinite;
}

.cloud-2 {
  right: -180px;
  top: 34%;
  width: 620px;
  height: 260px;
  animation: cloudMove2 22s ease-in-out infinite;
}

.cloud-3 {
  left: -160px;
  top: 58%;
  width: 700px;
  height: 280px;
  animation: cloudMove3 26s ease-in-out infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.55); }
  70% { box-shadow: 0 0 0 10px rgba(230, 162, 60, 0); }
  100% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0); }
}

@keyframes floatSeal {
  0%, 100% { transform: translateY(0) rotate(-6deg); }
  50% { transform: translateY(-10px) rotate(-6deg); }
}

@keyframes drift {
  0% { transform: translate3d(0, 0, 0); }
  100% { transform: translate3d(-60px, 30px, 0); }
}

@keyframes cloudMove1 {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(240px); }
}

@keyframes cloudMove2 {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(-260px); }
}

@keyframes cloudMove3 {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(300px); }
}
</style>
