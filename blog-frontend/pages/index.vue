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
    <div v-if="loading" class="columns-2 md:columns-3 lg:columns-4 gap-5">
      <div v-for="i in 8" :key="i" class="break-inside-avoid mb-5">
        <div class="skeleton rounded-card" :style="{ height: (140 + (i % 3) * 40) + 'px' }" />
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

    <!-- JS Masonry Waterfall -->
    <template v-else>
      <div ref="masonryContainer" class="relative" :style="{ height: containerHeight + 'px' }">
        <div
          v-for="(article, index) in articles"
          :key="article.id"
          data-card
          class="absolute transition-all duration-300 ease-out"
          :style="cardStyle(index)"
        >
          <ArticleCard :article="article" />
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
const masonryContainer = ref<HTMLElement | null>(null)

// ========== Masonry Layout ==========
const columnCount = ref(4)
const gap = 20
const columnHeights = ref<number[]>([])
const containerWidth = ref(0)
const cardWidth = ref(0)
const containerHeight = ref(0)
const cardPositions = ref<{ left: number; top: number; width: number; height: number }[]>([])
let resizeObserver: ResizeObserver | null = null

function getColumnCount() {
  if (typeof window === 'undefined') return 4
  const w = window.innerWidth
  if (w < 640) return 2
  if (w < 1024) return 3
  return 4
}

function cardStyle(index: number) {
  const pos = cardPositions.value[index]
  if (!pos) return { visibility: 'hidden' as const }
  return {
    left: pos.left + 'px',
    top: pos.top + 'px',
    width: pos.width + 'px',
  }
}

async function layoutCards(startIndex = 0) {
  if (!masonryContainer.value) return

  // Initialize column heights and container width on first layout
  if (startIndex === 0) {
    columnCount.value = getColumnCount()
    containerWidth.value = masonryContainer.value.clientWidth
    cardWidth.value = (containerWidth.value - gap * (columnCount.value - 1)) / columnCount.value
    columnHeights.value = new Array(columnCount.value).fill(0)
    cardPositions.value = new Array(articles.value.length)
  }

  // Layout each card starting from startIndex
  for (let i = startIndex; i < articles.value.length; i++) {
    // Find shortest column
    const col = columnHeights.value.indexOf(Math.min(...columnHeights.value))
    const left = col * (cardWidth.value + gap)
    const top = columnHeights.value[col]

    cardPositions.value[i] = {
      left,
      top,
      width: cardWidth.value,
      height: 0, // will be updated by ResizeObserver
    }

    // Estimate height based on content (will be refined by ResizeObserver)
    const estimatedHeight = estimateCardHeight(articles.value[i])
    columnHeights.value[col] += estimatedHeight + gap
  }

  containerHeight.value = Math.max(...columnHeights.value, 100)
}

function estimateCardHeight(article: Article): number {
  // Rough estimate: base + image area + text
  let h = 120 // padding + meta + title
  if (article.coverImage) {
    h += cardWidth.value * 0.75 // image at ~4:3 ratio
  }
  if (article.content) {
    h += Math.min(article.content.length / 3, 60)
  }
  return h
}

function measureRealHeights() {
  if (!masonryContainer.value) return

  const cardEls = masonryContainer.value.querySelectorAll<HTMLElement>('[data-card]')
  cardEls.forEach((el, i) => {
    if (i < cardPositions.value.length && cardPositions.value[i]) {
      cardPositions.value[i].height = el.offsetHeight
    }
  })
}

// Re-layout on window resize (debounced)
let resizeTimer: ReturnType<typeof setTimeout> | null = null
function onResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    const newColCount = getColumnCount()
    if (newColCount !== columnCount.value && articles.value.length > 0) {
      columnCount.value = newColCount
      layoutCards(0)
      nextTick(() => measureRealHeights())
    }
  }, 200)
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
    if (page === 1) {
      articles.value = result.records || []
    } else {
      articles.value.push(...(result.records || []))
    }
    currentPage.value = page
    hasMore.value = result.records && result.records.length === pageSize

    // Layout cards after DOM update
    await nextTick()
    if (page === 1) {
      layoutCards(0)
    } else {
      layoutCards(articles.value.length - (result.records?.length || 0))
    }
    await nextTick()
    measureRealHeights()
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

// ========== Lifecycle ==========
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
    { rootMargin: '200px' },
  )
  observer.observe(sentinelRef.value)
}

watch([() => articles.value.length, activeTagId], () => {
  nextTick(() => {
    setupObserver()
  })
})

watch(activeTagId, () => {
  currentPage.value = 1
  hasMore.value = true
  fetchArticles(1)
})

onMounted(() => {
  fetchHotArticles()
  fetchTags()
  fetchArticles()
  window.addEventListener('resize', onResize)

  // Set up ResizeObserver on the container to catch image loads
  nextTick(() => {
    if (masonryContainer.value) {
      resizeObserver = new ResizeObserver(() => {
        measureRealHeights()
        // Recalculate column heights based on real measurements
        recalcColumnHeights()
      })
      resizeObserver.observe(masonryContainer.value)
    }
  })
})

onUnmounted(() => {
  observer?.disconnect()
  resizeObserver?.disconnect()
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})

function recalcColumnHeights() {
  // Recalculate container height from actual card bottoms (handles image loads)
  let maxBottom = 100
  for (let i = 0; i < cardPositions.value.length; i++) {
    const pos = cardPositions.value[i]
    if (!pos) continue
    const bottom = pos.top + pos.height + gap
    if (bottom > maxBottom) maxBottom = bottom
  }
  if (maxBottom !== containerHeight.value) {
    containerHeight.value = maxBottom
  }
}
</script>
