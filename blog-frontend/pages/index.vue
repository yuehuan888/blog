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
    <div v-if="loading" class="flex flex-wrap gap-4">
      <div
        v-for="i in 8"
        :key="i"
        class="skeleton rounded-card"
        :style="{ width: `calc(${100 / columnCount}% - ${gap * (columnCount - 1) / columnCount}px - 1px)`, height: (140 + (i % 3) * 40) + 'px' }"
      />
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
      <div ref="masonryRef" class="masonry-container relative w-full" :style="{ height: containerHeight + 'px' }">
        <div
          v-for="(article, i) in articles"
          :key="article.id"
          class="masonry-item absolute transition-all duration-300"
          :style="getCardStyle(i)"
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

// ========== Masonry Layout ==========
const masonryRef = ref<HTMLElement | null>(null)
const columnCount = ref(3)
const gap = 16
const containerHeight = ref(600)

interface CardPos {
  left: string
  top: string
  width: string
}

const cardPositions = ref<CardPos[]>([])

function updateColumnCount() {
  if (!import.meta.client) return
  const w = window.innerWidth
  const oldCount = columnCount.value
  if (w < 640) columnCount.value = 2
  else if (w < 1024) columnCount.value = 3
  else columnCount.value = 4
  if (oldCount !== columnCount.value) {
    nextTick(() => layoutCards())
  }
}

function getColumnWidth(): number {
  if (!masonryRef.value) return 280
  const w = masonryRef.value.clientWidth
  if (w <= 0) return 280
  return (w - gap * (columnCount.value - 1)) / columnCount.value
}

function estimateCardHeight(article: Article): number {
  let h = 120 // base: title + meta + padding
  if (article.coverImage) {
    h = 250 // image card
  }
  // Use deterministic variation based on article id
  const variation = article.id ? (article.id % 3) * 15 : 0
  return h + variation
}

function layoutCards() {
  const n = articles.value.length
  if (n === 0 || columnCount.value === 0) {
    containerHeight.value = 0
    return
  }

  const colWidth = getColumnWidth()
  const heights = new Array(columnCount.value).fill(0)
  const positions: CardPos[] = []

  for (let i = 0; i < n; i++) {
    // Find shortest column
    let minCol = 0
    for (let c = 1; c < columnCount.value; c++) {
      if (heights[c] < heights[minCol]) minCol = c
    }

    const cardHeight = estimateCardHeight(articles.value[i])
    positions.push({
      left: (minCol * (colWidth + gap)) + 'px',
      top: heights[minCol] + 'px',
      width: colWidth + 'px',
    })
    heights[minCol] += cardHeight + gap
  }

  cardPositions.value = positions
  containerHeight.value = Math.max(...heights, 200)
}

function getCardStyle(index: number): Record<string, string> {
  if (index < cardPositions.value.length) {
    return cardPositions.value[index] as Record<string, string>
  }
  return { left: '0px', top: '0px', width: '200px' }
}

function handleResize() {
  updateColumnCount()
}

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
    layoutCards()
    setupObserver()
  })
})

// 切换标签时重置列表
watch(activeTagId, () => {
  currentPage.value = 1
  hasMore.value = true
  fetchArticles(1).then(() => nextTick(() => layoutCards()))
})

onMounted(() => {
  updateColumnCount()
  fetchHotArticles()
  fetchTags()
  fetchArticles().then(() => nextTick(() => layoutCards()))
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('resize', handleResize)
})
</script>
