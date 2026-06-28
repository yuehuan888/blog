<template>
  <div v-if="articles.length > 0" class="mb-6">
    <div class="flex items-center gap-2 mb-3">
      <span class="text-lg">🔥</span>
      <h3 class="text-base font-bold">热门推荐</h3>
      <NuxtLink to="/article/hot" class="text-xs text-primary ml-auto hover:underline">
        查看全部 →
      </NuxtLink>
    </div>
    <div class="flex gap-3 overflow-x-auto pb-2 scrollbar-hide">
      <NCard
        v-for="item in articles"
        :key="item.articleId"
        size="small"
        :bordered="false"
        class="flex-shrink-0 w-40 cursor-pointer border-l-4 border-primary-light hover:shadow-md transition-shadow"
        @click="navigateTo(item.type === 'video' ? `/video/${item.articleId}` : `/article/${item.articleId}`)"
      >
        <p class="text-sm font-medium line-clamp-2 text-text-primary">{{ item.title }}</p>
        <p class="text-xs text-text-secondary mt-1">👁 {{ item.readCount }} 次阅读</p>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NCard } from 'naive-ui'
import type { HotArticleDTO } from '~/types'

defineProps<{
  articles: HotArticleDTO[]
}>()
</script>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
