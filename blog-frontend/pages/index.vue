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

    <!-- Skeleton Loading -->
    <div v-if="loading" class="flex gap-5 items-start">
      <div v-for="col in colCount" :key="col" class="flex-1 flex flex-col gap-5">
        <div v-for="i in 3" :key="i" class="skeleton rounded-card" :style="{ height: (140 + ((col + i) % 3) * 50) + 'px' }" />
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="flex flex-col items-center py-20">
      <NResult status="error" title="加载失败" :description="error">
        <template #footer>
          <NButton type="primary" @click="fetchArticles()">重新加载</NButton>
        </template>
      </NResult>
    </div>

    <!-- Empty -->
    <div v-else-if="articles.length === 0" class="py-20">
      <EmptyState
        description="还没有文章，成为第一个创作者吧～"
        action-label="写文章"
        @action="navigateTo('/article/write')"
      />
    </div>

    <!-- Multi-column waterfall: cards assigned to columns, never move between columns -->
    <template v-else>
      <div class="flex gap-5 items-start">
        <div
          v-for="(colCards, colIdx) in columnCards"
          :key="colIdx"
          class="flex-1 flex flex-col gap-5 min-w-0"
        >
          <ArticleCard
            v-for="article in colCards"
            :key="article.id"
            :article="article"
          />
        </div>
      </div>

      <!-- Loading more spinner -->
      <div v-if="loadingMore" class="flex justify-center py-8">
        <NSpin size="small" />
      </div>

      <p v-else-if="!hasMore" class="text-center text-text-secondary text-sm mt-8">
        — 已经到底了 —
      </p>

      <!-- IntersectionObserver sentinel -->
      <div v-if="hasMore && !loadingMore" ref="sentinelRef" class="h-1" />
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

// ========== Column distribution ==========
const colCount = ref(4)
// Each column holds its own list of articles — once assigned, a card stays in its column forever
const columnCards = ref<Article[][]>([[], [], [], []])

function updateColCount() {
  if (typeof window === 'undefined') { colCount.value = 4; return }
  const w = window.innerWidth
  if (w < 640) colCount.value = 2
  else if (w < 1024) colCount.value = 3
  else colCount.value = 4
}

function distributeToColumns(newArticles: Article[], isReset: boolean) {
  if (isReset) {
    // Reset: clear all columns, redistribute everything
    columnCards.value = Array.from({ length: colCount.value }, () => [])
    for (const article of newArticles) {
      // Find column with fewest cards
      const shortest = columnCards.value.reduce((best, col, i) =>
        col.length < columnCards.value[best].length ? i : best, 0)
      columnCards.value[shortest].push(article)
    }
  } else {
    // Append: distribute new cards to shortest columns
    for (const article of newArticles) {
      const shortest = columnCards.value.reduce((best, col, i) =>
        col.length < columnCards.value[best].length ? i : best, 0)
      columnCards.value[shortest].push(article)
    }
  }
}

// ========== Data Fetching ==========
async function fetchHotArticles() {
  try {
    const result = await getHotArticles()
    hotArticles.value = result.records || []
  } catch {
    // Non-critical
  }
}

async function fetchArticles(page = 1) {
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
    const newRecords = result.records || []

    if (page === 1) {
      articles.value = newRecords
      distributeToColumns(newRecords, true)
      loading.value = false
    } else {
      articles.value.push(...newRecords)
      distributeToColumns(newRecords, false)
      loadingMore.value = false
    }

    currentPage.value = page
    hasMore.value = newRecords.length === pageSize
  } catch (err: any) {
    if (page === 1) {
      loading.value = false
      error.value = err.message || '加载文章失败'
    } else {
      loadingMore.value = false
    }
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

// ========== Lifecycle ==========
let observer: IntersectionObserver | null = null

function setupObserver() {
  if (observer) observer.disconnect()
  if (!sentinelRef.value) return

  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) loadMore()
    },
    { rootMargin: '200px' },
  )
  observer.observe(sentinelRef.value)
}

watch([() => articles.value.length, activeTagId], () => {
  nextTick(() => setupObserver())
})

watch(activeTagId, () => {
  currentPage.value = 1
  hasMore.value = true
  fetchArticles(1)
})

// Handle responsive column count
function onResize() {
  const prev = colCount.value
  updateColCount()
  if (colCount.value !== prev && articles.value.length > 0) {
    // Redistribute ALL articles with new column count
    distributeToColumns(articles.value, true)
  }
}

onMounted(() => {
  updateColCount()
  fetchHotArticles()
  fetchTags()
  fetchArticles()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('resize', onResize)
})
</script>
