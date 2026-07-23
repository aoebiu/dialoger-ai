<template>
  <div class="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useConversationStore } from '@/stores/conversation'

const router = useRouter()
const theme = useThemeStore()
const conv = useConversationStore()

// 初始化主题
theme.initTheme()

// 监听并应用主题变化
watch(
  () => theme.resolvedTheme,
  (newTheme) => {
    document.documentElement.setAttribute('data-theme', newTheme)
  },
  { immediate: true }
)

// 计算当前对话标题
const currentTitle = computed(() =>
  conv.conversations.find(c => c.id === conv.currentId)?.title ?? ''
)

// 计算页面标题
const pageTitle = computed(() => {
  const route = router.currentRoute.value
  const routeName = route.name

  if (routeName === 'chat' && currentTitle.value) {
    return `${currentTitle.value} - Dialoger AI`
  }

  // 检查是否在设置页面或其子路由中
  const inSettingsPage = route.matched.some(r => r.path.startsWith('/settings'))
  if (inSettingsPage) {
    return '设置 - Dialoger AI'
  }

  // 其他页面的标题映射
  const titleMap: Record<string, string> = {
    'login': '登录',
  }

  const title = titleMap[String(routeName)] || 'Dialoger AI'
  return `${title} - Dialoger AI`
})

// 监听页面标题变化，更新浏览器标题
watch(
  pageTitle,
  (newTitle) => {
    document.title = newTitle
  },
  { immediate: true }
)
</script>

<style>
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: system-ui, "Segoe UI", sans-serif;
    background: var(--color-bg-page);
    color: var(--color-text-primary);
    transition: background-color 0.3s ease, color 0.3s ease;
  }
  .app {
    height: 100vh;
    overflow: hidden;
  }
</style>
