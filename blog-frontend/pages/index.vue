<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <!-- Banner -->
    <div
      class="rounded-2xl p-8 mb-6 text-white"
      style="background: linear-gradient(135deg, #2D6A4F 0%, #52B788 100%);"
    >
      <h1 class="text-2xl md:text-3xl font-bold mb-2">发现好内容 🌿</h1>
      <p class="text-white/80 text-sm md:text-base">
        每日精选 · 热门话题 · 创作你的故事
      </p>
    </div>

    <!-- Hot Articles Horizontal Scroll -->
    <HotScroll :articles="hotArticles" />

    <!-- Tag Filter Row -->
    <div class="flex items-center gap-2 mb-6 flex-wrap">
      <NTag
        :type="activeTagId === null ? 'success' : 'default'"
        :bordered="false"
        class="cursor-pointer"
        @click="activeTagId = null"
      >
        全部
      </NTag>
      <NTag
        v-for="tag in tags"
        :key="tag.id"
        :type="activeTagId === tag.id ? 'success' : 'default'"
        :bordered="false"
        class="cursor-pointer"
        @click="activeTagId = tag.id"
      >
        {{ tag.name }}
      </NTag>
      <NuxtLink
        to="/tag"
        class="text-xs text-primary ml-1 hover:underline flex-shrink-0"
      >
        更多标签 →
      </NuxtLink>
    </div>

    <!-- Article Waterfall Grid -->
    <div v-if="loading" class="columns-2 md:columns-3 lg:columns-4 gap-5">
      <div v-for="i in 8" :key="i" class="break-inside-avoid mb-5">
        <div class="skeleton rounded-card" :style="{ height: (140 + (i % 3) * 40) + 'px' }" />
      </div>
    </div>

    <div v-else-if="error" class="flex flex-col items-center py-20">
      <NResult status="error" title="加载失败" :description="error">
        <template #footer>
          <NButton type="primary" @click="fetchArticles()">重新加载</NButton>
        </template>
      </NResult>
    </div>

    <div v-else-if="articles.length === 0" class="py-20">
      <EmptyState
        description="还没有文章，成为第一个创作者吧～"
        action-label="写文章"
        @action="navigateTo('/article/write')"
      />
    </div>

    <template v-else>
      <div class="columns-2 md:columns-3 lg:columns-4 gap-5">
        <div
          v-for="article in articles"
          :key="article.id"
          class="break-inside-avoid mb-5"
        >
          <ArticleCard :article="article" />
        </div>
      </div>

      <!-- 滚动加载指示器 -->
      <div v-if="loadingMore" class="flex justify-center py-8">
        <NSpin size="small" />
      </div>

      <p v-else-if="!hasMore" class="text-center text-text-secondary text-sm mt-8">
        — 已经到底了 —
      </p>

      <!-- IntersectionObserver 哨兵 -->
      <div
        v-if="hasMore && !loadingMore"
        ref="sentinelRef"
        class="h-1"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { NTag, NButton, NResult, NSpin } from 'naive-ui'
import { getArticles, getHotArticles } from '~/api/modules/article'
import { getTagCloud } from '~/api/modules/tag'
import type { Article, HotArticleDTO, TagCloudItem } from '~/types'

const articles = ref<Article[]>([])
const hotArticles = ref<HotArticleDTO[]>([])
const tags = ref<TagCloudItem[]>([])
const activeTagId = ref<number | null>(null)
const loading = ref(true)
const loadingMore = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = 12
const sentinelRef = ref<HTMLElement | null>(null)

async function fetchHotArticles() {
  try {
    const result = await getHotArticles()
    hotArticles.value = result.records || []
  } catch {
    // Non-critical, silently fail
  }
}

async function fetchArticles(page = 1) {
  // 首次加载显示全屏骨架，追加加载不替换 DOM
  if (page === 1) {
    loading.value = true
    error.value = null
  }

  try {
    const params: Record<string, any> = {
      page,
      size: pageSize,
      status: 'published',
    }
    if (activeTagId.value) {
      params.tagId = activeTagId.value
    }

    const result = await getArticles(params)
    if (page === 1) {
      articles.value = result.records || []
    } else {
      articles.value.push(...(result.records || []))
    }
    currentPage.value = page
    hasMore.value = result.records && result.records.length === pageSize
  } catch (err: any) {
    if (page === 1) {
      error.value = err.message || '加载文章失败'
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  await fetchArticles(currentPage.value + 1)
}

async function fetchTags() {
  try {
    tags.value = (await getTagCloud('count')).slice(0, 20)
  } catch {
    // Non-critical
  }
}

// ========== 无限滚动 ==========

let observer: IntersectionObserver | null = null

function setupObserver() {
  if (observer) observer.disconnect()
  if (!sentinelRef.value) return

  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) {
        loadMore()
      }
    },
    { rootMargin: '200px' }, // 提前 200px 触发，用户无感知
  )
  observer.observe(sentinelRef.value)
}

// 每次 articles 更新后或切换标签后重新挂载哨兵
watch([() => articles.value.length, activeTagId], () => {
  nextTick(() => {
    setupObserver()
  })
})

// 切换标签时重置列表
watch(activeTagId, () => {
  currentPage.value = 1
  hasMore.value = true
  fetchArticles(1)
})

onMounted(() => {
  fetchHotArticles()
  fetchTags()
  fetchArticles()
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>
