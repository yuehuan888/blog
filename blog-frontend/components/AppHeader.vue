<template>
  <header class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
    <div class="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
      <!-- Logo -->
      <NuxtLink to="/" class="flex items-center gap-2 text-primary font-bold text-lg no-underline">
        <span class="text-xl">🌿</span>
        <span>GreenRead</span>
      </NuxtLink>

      <!-- Nav Links -->
      <nav class="hidden md:flex items-center gap-6">
        <NuxtLink to="/" class="text-sm text-text-secondary hover:text-primary transition-colors">
          首页
        </NuxtLink>
        <NuxtLink to="/tag/index" class="text-sm text-text-secondary hover:text-primary transition-colors">
          标签云
        </NuxtLink>
        <NuxtLink to="/article/hot" class="text-sm text-text-secondary hover:text-primary transition-colors">
          🔥 热门
        </NuxtLink>
      </nav>

      <!-- Right Actions -->
      <div class="flex items-center gap-3">
        <template v-if="authStore.isLoggedIn">
          <NButton text @click="navigateTo('/article/write')">
            <template #icon>
              <NIcon><PencilOutline /></NIcon>
            </template>
            写文章
          </NButton>
          <NDropdown trigger="click" :options="userMenuOptions" @select="handleUserMenu">
            <span class="cursor-pointer inline-block">
              <UserAvatar
                :username="authStore.user?.nickname || authStore.user?.username"
                :src="authStore.user?.avatar"
                size="small"
              />
            </span>
          </NDropdown>
          <span class="hidden md:block text-sm text-text-secondary max-w-24 truncate">
            {{ authStore.user?.nickname || authStore.user?.username }}
          </span>
        </template>
        <template v-else>
          <NButton text @click="navigateTo('/user/login')">登录</NButton>
          <NButton type="primary" size="small" @click="navigateTo('/user/register')">注册</NButton>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { NButton, NDropdown, NIcon } from 'naive-ui'
import { PencilOutline } from '@vicons/ionicons5'
import { useAuthStore } from '~/stores/auth'

const authStore = useAuthStore()

const userMenuOptions = computed(() => {
  const options: any[] = [
    {
      label: '个人主页',
      key: 'profile',
    },
  ]
  if (authStore.isAdmin) {
    options.push(
      { label: '标签管理', key: 'admin-tags' },
      { label: '评论管理', key: 'admin-comments' },
    )
  }
  options.push(
    { type: 'divider' as const, key: 'd1' },
    { label: '退出登录', key: 'logout' },
  )
  return options
})

function handleUserMenu(key: string) {
  switch (key) {
    case 'profile':
      navigateTo(`/user/${authStore.user!.userId}`)
      break
    case 'admin-tags':
      navigateTo('/admin/tags')
      break
    case 'admin-comments':
      navigateTo('/admin/comments')
      break
    case 'logout':
      authStore.logout()
      navigateTo('/')
      break
  }
}
</script>
